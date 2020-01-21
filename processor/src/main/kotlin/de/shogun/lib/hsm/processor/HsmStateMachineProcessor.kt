package de.shogun.lib.hsm.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.StateMachine
import de.shogun.lib.hsm.TransitionKind
import de.shogun.lib.hsm.annotations.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class HsmStateMachineProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        fun getStateSetClass(): ParameterizedTypeName {
            val setClass = ClassName("kotlin.collections", "Set")
            val stateClass = ClassName(State::class.java.`package`.name, State::class.java.simpleName)
            return setClass.parameterizedBy(stateClass)
        }
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(
            HsmState::class.java.name,
            HsmTransitionExternal::class.java.name
    )

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val stateElements = roundEnv.getElementsAnnotatedWith(HsmState::class.java)

        stateElements.asSequence().groupBy {
            val annotation = it.annotationMirrors.find { annotation ->
                annotation.annotationType.toString() == HsmState::class.qualifiedName
            } ?: return false
            getAnnotationValue<String>("stateMachineName", annotation)
                    ?: throw IllegalArgumentException("HsmState Annotation is missing a Statemachine Name")
        }.forEach { (name, elements) ->
            stateElements.firstOrNull()?.let {
                val pack = processingEnv.elementUtils.getPackageOf(it).toString()
                buildCreateStateMachineFactory(name, pack, elements)
            }
        }

        return true
    }

    // region code generation

    private fun buildCreateStateMachineFactory(
            name: String,
            pack: String,
            elements: List<Element>
    ) {
        val fileName = name + "HsmFactory"
        val fileBuilder = FileSpec.builder(pack, fileName)
        val objectBuilder = TypeSpec.objectBuilder(fileName)

        objectBuilder.addProperty(buildStateMachineProperty())
        objectBuilder.addFunction(buildCreateStateFunction(elements.toMutableList()))
        objectBuilder.addFunction(buildCreateStateTransitionsFunction(elements))
        objectBuilder.addFunction(buildFindStateFunction())

        fileBuilder.addType(objectBuilder.build())

        val file = fileBuilder.build()
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    private fun buildStateMachineProperty(): PropertySpec =
            PropertySpec.builder("stateMachine", StateMachine::class)
                    .delegate("""
                    |lazy {
                    |    val states = createStates()
                    |    createStateTransitions(states)
                    |    StateMachine(states.first(), *states.toTypedArray())
                    |}
                """.trimMargin()).build()

    private fun buildCreateStateFunction(stateElements: MutableList<Element>): FunSpec {
        val funBuilder = FunSpec.builder("createStates")
        funBuilder.addStatement("val states = mutableSetOf<%T>()", State::class)

        val initialState = stateElements.find {
            val annotation = it.annotationMirrors.find { annotation ->
                isStateAnnotation(annotation)
            } ?: throw IllegalArgumentException("No HSM states declared.")
            getAnnotationValue<Boolean>("isInitialState", annotation) != null
        } ?: throw IllegalArgumentException("No state as initial state declared")

        stateElements.removeAt(stateElements.indexOf(initialState))
        stateElements.add(0, initialState)

        stateElements.forEach {
            val pack = processingEnv.elementUtils.getPackageOf(it).toString()
            val stateClass = ClassName(pack, it.simpleName.toString())
            funBuilder.addStatement("states.add(%T())", stateClass)
        }
        funBuilder.addStatement("return states")
        funBuilder.addModifiers(KModifier.PRIVATE)

        val setClass = ClassName("kotlin.collections", "Set")
        val stateClass = ClassName(State::class.java.`package`.name, State::class.java.simpleName)
        val setOfStatesClass = setClass.parameterizedBy(stateClass)

        funBuilder.returns(setOfStatesClass)
        return funBuilder.build()
    }

    private fun buildCreateStateTransitionsFunction(stateElements: List<Element>): FunSpec {
        val funBuilder = FunSpec.builder("createStateTransitions")
        funBuilder.addParameter("states", getStateSetClass())
        funBuilder.addModifiers(KModifier.PRIVATE)

        stateElements.forEach { state ->
            state.getAnnotation(HsmTransitionExternal::class.java)?.transitions?.let { transitions ->
                buildAddHandlerStatement(funBuilder, TransitionKind.External, transitions, state)
            }
            state.getAnnotation(HsmTransitionInternal::class.java)?.transitions?.let { transitions ->
                buildAddHandlerStatement(funBuilder, TransitionKind.Internal, transitions, state)
            }
            state.getAnnotation(HsmTransitionLocal::class.java)?.transitions?.let { transitions ->
                buildAddHandlerStatement(funBuilder, TransitionKind.Local, transitions, state)
            }
        }
        return funBuilder.build()
    }

    private fun buildAddHandlerStatement(
            funBuilder: FunSpec.Builder,
            transitionKind: TransitionKind,
            transitions: Array<HsmTransition>,
            sourceState: Element
    ) {
        transitions.forEach { transition ->
            val targetStateName = (transition.getTargetState() as DeclaredType).asElement().simpleName.toString()

            val actionElement = sourceState.enclosedElements.find {
                val annotation = it.getAnnotation(HsmAction::class.java) ?: return@find false
                annotation.eventName == transition.eventName
            }

            val guardElement = sourceState.enclosedElements.find {
                val annotation = it.getAnnotation(HsmGuard::class.java) ?: return@find false
                annotation.eventName == transition.eventName
            }

            val action = if (actionElement != null) "::${actionElement.simpleName}" else null
            val guard = if (guardElement != null) "::${guardElement.simpleName}" else null

            funBuilder.addStatement("""
                states.findState("${sourceState.simpleName}").apply {
                    this as ${sourceState.simpleName}
                     addHandler(
                        "${transition.eventName}", 
                        states.findState("$targetStateName"),
                        %1T.$transitionKind,
                        $action,
                        $guard
                     )
                }
            """.trimIndent(), TransitionKind::class)
        }
    }

    private fun buildFindStateFunction(): FunSpec {
        val funBuilder = FunSpec.builder("findState")
        funBuilder.receiver(getStateSetClass())
        funBuilder.returns(State::class)
        funBuilder.addModifiers(KModifier.PRIVATE)
        funBuilder.addParameter("stateName", String::class)
        funBuilder.addStatement("""
            |return find {
            |   it.javaClass.simpleName == stateName
            |}!!
        """.trimMargin())
        return funBuilder.build()
    }

    // endregion

    // region Helper

    private fun HsmTransition.getTargetState(): TypeMirror? {
        try {
            this.targetState
        } catch (e: MirroredTypeException) {
            return e.typeMirror
        }
        return null
    }

    private fun <T> getAnnotationValue(fieldName: String, annotation: AnnotationMirror): T? {
        val key = annotation.elementValues?.keys?.find { key ->
            key.simpleName.toString() == fieldName
        }
        val value = annotation.elementValues?.get(key)?.value

        @Suppress("UNCHECKED_CAST")
        return value as? T
    }

    private fun isStateAnnotation(annotation: AnnotationMirror): Boolean =
            annotation.annotationType.toString() == HsmState::class.qualifiedName.toString()
    // endregion
}