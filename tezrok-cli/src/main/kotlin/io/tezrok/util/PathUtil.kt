package io.tezrok.util

import java.net.URL
import java.nio.file.Path

fun Path.toURL(): URL = toUri().toURL()
