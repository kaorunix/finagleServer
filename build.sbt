name := "finagleServer"

version := "0.01"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "1.7.1" % "test",
        "com.twitter" % "finagle-core" % "1.11.1",
        "com.twitter" % "finagle-http" % "1.11.1",
        "com.twitter" % "util-core" % "1.12.13"
)

seq(ProguardPlugin.proguardSettings :_*)

proguardOptions += keepMain("http.HttpServer")

