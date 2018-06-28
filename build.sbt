name := "queryprotected"
  
version := "0.9"
  
scalaVersion := "2.11.8"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.5" % "runtime",
  "org.eclipse.rdf4j" % "rdf4j-runtime" % "2.2.2",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "commons-logging" % "commons-logging" % "1.2",
  "org.ddahl" %% "rscala" % "2.5.3"
)

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-jdk14")) }
