package com.github.rosecky.shacl2plantuml.lib.definition

import java.lang.Exception

class UnresolvableUriException(val uri: String, message: String) : Exception(message) {
}