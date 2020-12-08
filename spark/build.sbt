name := "smile-spark"

// Spark doesn't support 2.13+
scalaVersion := "2.12.12"

libraryDependencies ++= {
  val sparkV = "2.4.7"
  Seq(
    "org.apache.spark" %% "spark-core"  % sparkV % Provided,
    "org.apache.spark" %% "spark-sql"   % sparkV % Provided,
    "org.apache.spark" %% "spark-mllib" % sparkV % Provided,
    "org.specs2"       %% "specs2-core" % "4.10.5" % Test,
    "org.bytedeco" % "javacpp"            % "1.5.4"        % "test",
    "org.bytedeco" % "openblas"           % "0.3.10-1.5.4" % "test",
    "org.bytedeco" % "openblas-platform"  % "0.3.10-1.5.4" % "test",
    "org.bytedeco" % "arpack-ng"          % "3.7.0-1.5.4"  % "test",
    "org.bytedeco" % "arpack-ng-platform" % "3.7.0-1.5.4"  % "test"
  )
}
