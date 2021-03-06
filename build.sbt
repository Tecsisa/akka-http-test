name := "akka-http-test"

version := "1.0.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaVersion = "2.3.12"
  val akkaStreamVersion = "1.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaStreamVersion,
    "com.hunorkovacs" %% "koauth" % "1.1.0",
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaStreamVersion % Test,
    "org.scalatest" %% "scalatest" % "2.2.4" % Test
  )
}

scalacOptions ++= Seq("-feature", "-language:higherKinds", "-language:implicitConversions", "-deprecation", "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8")

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

lazy val root = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)

// enable updating file headers //

import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt.Keys._

headers := Map(
  "scala" -> Apache2_0("2015", "Dennis Vriend"),
  "conf" -> Apache2_0("2015", "Dennis Vriend", "#")
)

// enable scala code formatting //

import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable sbt-revolver

import spray.revolver.RevolverPlugin.Revolver

Revolver.settings ++ Seq(
  Revolver.enableDebugging(port = 5050, suspend = false),
  mainClass in Revolver.reStart := Some("com.github.dnvriend.SimpleServer")
)
