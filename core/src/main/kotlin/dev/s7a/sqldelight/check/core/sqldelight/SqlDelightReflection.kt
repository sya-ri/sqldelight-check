package dev.s7a.sqldelight.check.core.sqldelight

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ServiceLoader

internal const val SQL_DELIGHT_COMPILATION_UNIT_CLASS = "app.cash.sqldelight.core.SqlDelightCompilationUnit"
internal const val SQL_DELIGHT_DATABASE_PROPERTIES_CLASS = "app.cash.sqldelight.core.SqlDelightDatabaseProperties"
internal const val SQL_DELIGHT_DIALECT_CLASS = "app.cash.sqldelight.dialect.api.SqlDelightDialect"
internal const val SQL_DELIGHT_ENVIRONMENT_CLASS = "app.cash.sqldelight.core.SqlDelightEnvironment"
internal const val SQL_DELIGHT_SOURCE_FOLDER_CLASS = "app.cash.sqldelight.core.SqlDelightSourceFolder"

internal fun ClassLoader.loadDialect(): Any? {
    val dialectClass = Class.forName(SQL_DELIGHT_DIALECT_CLASS, true, this)
    return ServiceLoader.load(dialectClass, this).firstOrNull()
}

internal fun ClassLoader.proxy(
    interfaceName: String,
    values: Map<String, Any?>,
): Any {
    val interfaceClass = Class.forName(interfaceName, true, this)
    return Proxy.newProxyInstance(this, arrayOf(interfaceClass)) { proxy, method, arguments ->
        when (method.name) {
            "equals" -> proxy === arguments?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "${interfaceClass.simpleName}${values}"
            else -> values[method.name]
        }
    }
}

internal fun Any.invokeNoArg(name: String): Iterable<*> {
    val method: Method = javaClass.methods.first { candidate -> candidate.name == name && candidate.parameterCount == 0 }
    return method.invoke(this) as Iterable<*>
}
