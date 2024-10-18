package com.thinkiny.domain

import com.thinkiny.syntax.*

sealed trait Project:
  def excludes: Seq[String]
  def matches(s: List[FileState]): Boolean
  def matchFiles(s: List[FileState], fileNames: String*): Boolean =
    s.map(_.path.getFileName).exists(fileNames.contains(_))

case object ScalaProject extends Project:
  override def excludes: Seq[String] =
    Seq(".scala-build", ".metals", ".bsp", "*target", "*.bloop", "*metals.sbt")
  override def matches(s: List[FileState]): Boolean =
    matchFiles(s, "project.scala", "build.sbt")

case object JavaProject extends Project:
  override def excludes: Seq[String] =
    Seq("target", ".settings", ".classpath", ".factorypath")
  override def matches(s: List[FileState]): Boolean =
    matchFiles(s, "pom.xml")

case object CppProject extends Project:
  override def excludes: Seq[String] =
    Seq("build", ".cache")
  override def matches(s: List[FileState]): Boolean =
    matchFiles(s, "CMakeLists.txt", "compile_commands.json")

object Project:
  def getExcludesFiles(root: List[FileState]): Seq[String] =
    val all = List(ScalaProject, JavaProject, CppProject)
    all.find(_.matches(root)) match
      case Some(p) => p.excludes
      case _       => Nil
