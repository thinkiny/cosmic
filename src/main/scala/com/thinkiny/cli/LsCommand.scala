package com.thinkiny.cli

import com.monovore.decline.Opts
import cats.effect.IO
import cats.syntax.all.*
import com.thinkiny.implicits.given
import com.thinkiny.service.location.LocationFile
import com.thinkiny.domain.FilePath
import com.thinkiny.domain.FileState
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

case class LsOptions(file: String, long: Boolean)

object LsCommand extends CliCommand[LsOptions, IO]:
  extension (s: String)
    def getFileName: String =
      s.substring(s.lastIndexOf('/') + 1)

  override def options: Opts[LsOptions] =
    Opts.subcommand("ls", "list files") {
      val long = Opts.flag("long", "use long format", "l").orFalse
      val file = Opts.argument[String]("FILE")
      (file, long).mapN(LsOptions.apply)
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
    else colorName

  def printListFiles(files: List[FileState], longFormat: Boolean): IO[Unit] =
    val dirsFirst = files.sortBy(s => (!s.isDir, s.path.getFileName))
    if longFormat then
      IO.println(dirsFirst.map(formatOutput(_, longFormat)).mkString("\n"))
    else IO.println(dirsFirst.map(formatOutput(_, longFormat)).mkString(" "))

  override def run(args: LsOptions): IO[Unit] =
    IO.pure(args.file)
      .map(FilePath(_))
      .flatMap:
        case x @ Some(p) => LocationFile[IO].getState(p).map(_.zip(x))
        case _           => IO.none
      .flatMap:
        case Some((s, p)) =>
          if s.isDir then
            LocationFile[IO].listFiles(p).flatMap(printListFiles(_, args.long))
          else IO.println(formatOutput(s, args.long))
        case _ => IO.println(s"no such file: ${args.file}")
