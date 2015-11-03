jarName in assembly := "final.jar"

name := "FacebookSimulator"

version := "0.1"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", 
					  "-deprecation")

resolvers ++= Seq(
	"RoundEights" at "http://maven.spikemark.net/roundeights",
 	"spray repo" at "http://repo.spray.io"
)

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.0",
    "com.roundeights" %% "hasher" % "1.2.0",
    "io.spray" %% "spray-can" % "1.3.3"
)







