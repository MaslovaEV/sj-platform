package com.bwsw.sj.crud.rest.utils

trait ValidationUtils {

  def validateName(name: String) = {
    name.matches("""^([a-z][a-z0-9-]*)$""")
  }

  def validateServiceNamespace(namespace: String) ={
    namespace.matches("""^([a-z][a-z0-9_]*)$""")
  }

  //todo create an abstract method - validate for all validators (which are object now)
}
