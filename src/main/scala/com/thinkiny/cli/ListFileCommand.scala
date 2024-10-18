package com.thinkiny.cli

import com.monovore.decline.Opts
import cats.effect.IO
import cats.syntax.all.*
import com.thinkiny.implicits.given
import com.thinkiny.service.location.LocationPath
import com.thinkiny.domain.FilePath
import com.thinkiny.domain.FileState
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.thinkiny.syntax.*

case class ListFileArgs(file: String, long: Boolean)

object ListFileCommand extends CliCommand[ListFileArgs, IO]:
  override def options: Opts[ListFileArgs] =
    Opts.subcommand("ls", "list files") {
      val long = Opts.flag("long", "use long format", "l").orFalse
      val file = Opts.argument[String]("FILE")
      (file, long).mapN(ListFileArgs.apply)
    }

  def blue(s: String): String =
    import Console.*
    s"${BLUE}${s}${RESET}"

  def formatDateTime(unixSeconds: Long): String =
    LocalDateTime
      .ofEpochSecond(unixSeconds, 0, ZoneOffset.ofHours(8))
      .format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      )

  def formatOutput(fs: FileState, long: Boolean): String =
    val fileName = fs.path.getFileName
    val colorName = if fs.isDir then blue(fileName) else fileName
    if long then
      val dir = if fs.isDir then "DIR" else "FIL"
      f"${dir}  ${fs.size.toString}%12s  ${formatDateTime(fs.modifyTime)}  ${colorName}"
    else
      val spaces = Math.max(1, 20 - fileName.size)
      colorName + s"${" " * spaces}"

  def printListFiles(files: List[FileState], longFormat: Boolean): IO[Unit] =
    val fileList =
      files.sortBy(s => (!s.isDir, s.path.getFileName.toLowerCase()))
    if longFormat then
      IO.println(fileList.map(formatOutput(_, longFormat)).mkString("\n"))
    else
      IO.println(
        fileList
          .grouped(5)
          .map(_.map(formatOutput(_, longFormat)).mkString)
          .mkString("\n")
      )

  override def run(args: ListFileArgs): IO[Unit] =
    IO.pure(args.file)
      .map(FilePath(_))
      .flatMap:
        case x @ Some(p) =>
          LocationPath[IO].getState(p).map(_.zip(x))
        case _ => IO.none
      .flatMap:
        case Some((s, p)) =>
          if s.isDir then
            LocationPath[IO].listFiles(p).flatMap(printListFiles(_, args.long))
          else IO.println(formatOutput(s, args.long))
        case _ => IO.println(s"no such file: ${args.file}")
