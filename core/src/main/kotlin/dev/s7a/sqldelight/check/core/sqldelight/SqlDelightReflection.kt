package dev.s7a.sqldelight.check.core.sqldelight

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ServiceLoader

/** SQLDelight compilation unit class name used for reflective proxies. */
internal const val SQL_DELIGHT_COMPILATION_UNIT_CLASS = "app.cash.sqldelight.core.SqlDelightCompilationUnit"

/** SQLDelight database properties class name used for reflective proxies. */
internal const val SQL_DELIGHT_DATABASE_PROPERTIES_CLASS = "app.cash.sqldelight.core.SqlDelightDatabaseProperties"

/** SQLDelight dialect service interface class name. */
internal const val SQL_DELIGHT_DIALECT_CLASS = "app.cash.sqldelight.dialect.api.SqlDelightDialect"

/** SQLDelight compiler environment class name. */
internal const val SQL_DELIGHT_ENVIRONMENT_CLASS = "app.cash.sqldelight.core.SqlDelightEnvironment"

/** SQLDelight source folder class name used for reflective proxies. */
internal const val SQL_DELIGHT_SOURCE_FOLDER_CLASS = "app.cash.sqldelight.core.SqlDelightSourceFolder"

/**
 * Loads the first SQLDelight dialect service visible to this class loader.
 */
internal fun ClassLoader.loadDialect(): Any? {
    val dialectClass = Class.forName(SQL_DELIGHT_DIALECT_CLASS, true, this)
    return ServiceLoader.load(dialectClass, this).firstOrNull()
}

/**
 * Creates a proxy for a SQLDelight value interface backed by method-name-to-value mappings.
 */
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

/**
 * Invokes a no-argument method that returns an iterable value.
 */
internal fun Any.invokeNoArg(name: String): Iterable<*> {
    val method: Method = javaClass.methods.first { candidate -> candidate.name == name && candidate.parameterCount == 0 }
    return method.invoke(this) as Iterable<*>
}
