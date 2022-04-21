/**
 * Precompiled [com.enpasos.muzero.kotlin-application-conventions.gradle.kts][Com_enpasos_muzero_kotlin_application_conventions_gradle] script plugin.
 *
 * @see Com_enpasos_muzero_kotlin_application_conventions_gradle
 */
class Com_enpasos_muzero_kotlinApplicationConventionsPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Com_enpasos_muzero_kotlin_application_conventions_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
