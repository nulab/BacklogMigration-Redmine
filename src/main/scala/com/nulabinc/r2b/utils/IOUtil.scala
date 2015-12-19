package com.nulabinc.r2b.utils

import scalax.file.Path

/**
  * @author uchida
  */
object IOUtil {

  def createDirectory(path: String) = {
    val dir: Path = Path.fromString(path)
    if (!dir.isDirectory) dir.createDirectory()
  }

  def input(strPath: String): Option[String] = {
    val path: Path = Path.fromString(strPath)
    if (path.isFile) Some(path.lines().mkString)
    else None
  }

  def output(path: String, content: String) = {
    val filePath: Path = Path.fromString(path).toAbsolute
    if (!filePath.isFile) filePath.createFile()
    filePath.write(content)
  }

  def copy(from: String, to: String) = {
    val in = Path.fromString(from)
    val out = Path.fromString(to)
    in.copyTo(out, replaceExisting = true)
  }

  def directoryPaths(strPath: String): Seq[Path] = {
    val directory: Path = Path.fromString(strPath)
    if (directory.isDirectory) directory.toAbsolute.children().toSeq
    else Seq.empty[Path]
  }

  def isDirectory(path: String): Boolean = {
    val filePath: Path = Path.fromString(path).toAbsolute
    filePath.isDirectory
  }

}
