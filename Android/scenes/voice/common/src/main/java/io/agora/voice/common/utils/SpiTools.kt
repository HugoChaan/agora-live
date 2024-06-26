package io.agora.voice.common.utils

import java.lang.RuntimeException
import java.util.*

object SpiTools {
    /**
     * Spi multiple load
     *
     * @param T
     * @param clazz
     * @return
     */
    fun <T> spiMultipleLoad(clazz: Class<T>): List<T> {
        val list: MutableList<T> = ArrayList()
        val serviceLoader: ServiceLoader<T> = ServiceLoader.load(clazz, clazz.classLoader)
        val it: Iterator<T> = serviceLoader.iterator()
        while (it.hasNext()) {
            list.add(it.next())
        }
        return list
    }

    /**
     * Spi single load
     *
     * @param T
     * @param clazz
     * @return
     */
    fun <T> spiSingleLoad(clazz: Class<T>): T? {
        var t: T? = null
        val serviceLoader: ServiceLoader<T> = ServiceLoader.load(clazz, clazz.classLoader)
        val it: Iterator<T> = serviceLoader.iterator()
        if (it.hasNext()) {
            if (t != null) {
                throw RuntimeException("SpiConstructor.spiSingleLoad: biz only need to implement " + clazz.simpleName)
            }
            t = it.next()
        }
        return t
    }
}