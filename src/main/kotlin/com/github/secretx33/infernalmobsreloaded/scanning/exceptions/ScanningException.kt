package com.github.secretx33.infernalmobsreloaded.scanning.exceptions

import java.lang.Exception
import java.lang.RuntimeException

class ScanningException(exception: Exception) : RuntimeException(exception)
