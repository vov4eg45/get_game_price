ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val circeVersion = "0.14.1"
lazy val doobieVersion = "1.0.0-RC1"
lazy val http4sVersion = "0.23.11"

lazy val root = (project in file("."))
  .settings(
    name := "coursework"
  )

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "net.ruippeixotog" %% "scala-scraper" % "2.2.1",
  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2"   % doobieVersion,
  "org.tpolecat" %% "doobie-hikari"   % doobieVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "uz.scala" %% "bot4scala" % "1.0.7"
)
