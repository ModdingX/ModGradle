package io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw.parser

import io.github.noeppi_noeppi.tools.modgradle.util.PackageMatcher

import java.io.Reader
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

object ModuleParser extends JavaTokenParsers {
  
  def parse(reader: Reader): ParsedModule = {
      parseAll(parsed_module, reader) match {
        case Success(x, _) => x
        case NoSuccess(msg, _) => throw new IllegalStateException("Failed to parse module file: " + msg)
      }
  }

  // Comments
  override protected val whiteSpace: Regex = "\\s*((#.*\r?\n)?\\s*)*".r
  
  def parsed_module: Parser[ParsedModule] = opt("open" | "exported") ~ "module" ~ name ~ ":" ~ rep(stmt) ^^ { case attr ~ _ ~ name ~ _ ~ statements => new ParsedModule(name, attr.contains("open"), attr.contains("exported"), statements.asJava) }
  
  def stmt: Parser[Statement] = (stmt_import | stmt_spi | stmt_require | stmt_exports | stmt_opens) <~ ";"
  
  def stmt_import: Parser[Statement] = "import" ~ import_name <~ ";" ^^ { case str1 ~ str2 => new ImportStatement(str1 + " " + str2) }
  def stmt_spi: Parser[Statement] = ("uses" | "provides") ~ "[^;]+".r <~ ";" ^^ { case str1 ~ str2 => new SpiStatement(str1 + " " + str2) }
  def stmt_require: Parser[Statement] = "requires" ~> opt("static") ~ opt("transitive") ~ name_list ^^ { case isStatic ~ isTransitive ~ modules => new RequireStatement(isStatic.nonEmpty, isTransitive.nonEmpty, modules) }
  def stmt_exports: Parser[Statement] = "exports" ~> stmt_pkg ^^ (x => new ExportsStatement(x._1, x._2))
  def stmt_opens: Parser[Statement] = "opens" ~> stmt_pkg ^^ (x => new OpensStatement(x._1, x._2))
  
  def stmt_pkg: Parser[(PackageMatcher, java.util.List[String])] = name_match_list ~ opt(stmt_pkg_but) ~ opt(stmt_pkg_to) ^^ { case packages ~ but ~ to => (new PackageMatcher(packages, but.getOrElse(java.util.List.of())), to.orNull) }
  def stmt_pkg_but: Parser[java.util.List[String]] = "but" ~> name_match_list
  def stmt_pkg_to: Parser[java.util.List[String]] = "to" ~> name_match_list
  
  def name_match_list: Parser[java.util.List[String]] = rep1sep(match_name, ',') ^^ (_.asJava)
  def name_list: Parser[java.util.List[String]] = rep1sep(name, ',') ^^ (_.asJava)
  def match_name: Parser[String] = rep1sep(ident | "**" | "*?" | "*", '.') ^^ (_.mkString(".")) | failure("Name expected.")
  def import_name: Parser[String] = repsep(ident, '.') ~ "." ~ (ident | "*") ^^ { case list ~ _ ~ last => list.mkString(".") + "." + last }
  def name: Parser[String] = rep1sep(ident, '.') ^^ (_.mkString(".")) | failure("Name expected.")
}
