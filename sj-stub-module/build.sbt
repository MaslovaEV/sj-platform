name := "sj-stub-module"

version := "0.1"

scalaVersion := "2.11.7"

assemblyMergeStrategy in assembly := {
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly := "sj-stub-module-test.jar"
