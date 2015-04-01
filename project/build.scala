import sbt._
import Keys._

/** These definitions partition Chisel into two components:
  * - core (main compiler)
  * - library (standard components)
  * All the work (except for testing) happens in the subprojects.
  * We use the root aggregate project for testing since there doesn't
  * appear to be a way to force sbt 0.13 to test subprojects sequentially,
  * despite the plethora of instructions on the net describing how to do so.
  * See https://github.com/sbt/sbt/issues/882
 */
object BuildSettings extends Build {

  // Common settings for all projects/subprojects
  val commonSettings = Defaults.defaultSettings ++ Seq (
    organization := "edu.berkeley.cs",
    // version := "2.2.26",
    version := "2.3-SNAPSHOT",
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4", "2.11.5"),

    /* Bumping "com.novocode" % "junit-interface" % "0.11", causes DelayTest testSeqReadBundle to fail
     *  in subtly disturbing ways on Linux (but not on Mac):
     *  - some fields in the generated .h file are re-named,
     *  - an additional field is added
     *  - the generated .cpp file has additional differences:
     *    - different temps in clock_lo
     *    - missing assignments
     *    - change of assignment order
     *    - use of "Tx" vs. "Tx.values"
     */
    libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test",
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),

    // Execute tests in the current project serially.
    // Tests from other projects may still run concurrently.
    parallelExecution in Test := false,
    scalacOptions ++= Seq("-deprecation", "-feature", "-language:reflectiveCalls", "-language:implicitConversions", "-language:existentials")
  ) ++ org.scalastyle.sbt.ScalastylePlugin.Settings

  // Common settings for all subprojects (where the real work happens).
  val commonArtifactSettings = commonSettings ++ Seq (
    //sourceDirectory := new File("@srcTop@"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    pomExtra := (
  <url>http://chisel.eecs.berkeley.edu/</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/ucb-bar/chisel.git</url>
    <connection>scm:git:github.com/ucb-bar/chisel.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jackbackrack</id>
      <name>Jonathan Bachrach</name>
      <url>http://people.csail.mit.edu/jrb/</url>
    </developer>
    <developer>
      <id>huytbvo</id>
      <name>Huy Vo</name>
    </developer>
  </developers>
      ),

    publishTo <<= version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    resolvers ++= Seq(
      "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
    )
  )

  lazy val core = (project in file("core")).
    settings(commonArtifactSettings: _*).
    settings(
      name := "chisel",
      test := {} /* no tests */
    )
  lazy val library = (project in file("library")).dependsOn(core).
    settings(commonArtifactSettings: _*).
    settings(
      name := "chisel_library",
      test := {} /* no tests */
    )

  lazy val root = (project in file(".")).
    aggregate(core, library).dependsOn(core % "test", library % "test").
    settings(commonSettings: _*).
    settings(
      // Don't publish anything in the root project.
      publishArtifact := false,
      publish := {},
      publishLocal := {},
      com.typesafe.sbt.pgp.PgpKeys.publishSigned := {},
      com.typesafe.sbt.pgp.PgpKeys.publishLocalSigned := {}
    )
}

