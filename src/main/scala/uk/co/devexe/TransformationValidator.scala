package uk.co.devexe

import java.io.{FilenameFilter, IOException, File}
import javax.xml.transform.TransformerException
import com.typesafe.config._
import com.typesafe.scalalogging.Logger
import org.apache.commons.io.FileUtils
import org.custommonkey.xmlunit.{Diff, DetailedDiff, Transform, XMLUnit}
import org.junit.Assert
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException

case class AppConf(inputDir: File, controlDir: File, styleSheetDir: File, stylesheetPrefix: String)
case class TransformConf(inputFile: File, controlFile: File, stylesheet: File)

object TransformationValidator {
  /** Whether to ignore whitespace when comparing the result to the control sample. */
  private val ignoreWhitespace: Boolean = true
  /** Whether to ignore attribute order when comparing the result to the control sample. */
  private val ignoreAttributeOrder: Boolean = true
  /** The name of the directory to store the transformation results in. */
  private val resultsDirectory: String = null

  def getAppConf: AppConf = {
    val conf = ConfigFactory.load()
    val inputDir = new File(conf.getString("test-conversion.samples.directory.input"))
    val controlDir = new File(conf.getString("test-conversion.samples.directory.control"))
    val stylesheetDir = new File(conf.getString("test-conversion.stylesheet.directory"))
    val stylesheetPrefix = conf.getString("test-conversion.stylesheet.filename.prefix")
    AppConf(inputDir, controlDir, stylesheetDir, stylesheetPrefix)
  }

  def getTransformConf(filename: String): TransformConf = {
    TransformConf(new File(getAppConf.inputDir, filename),
      new File(getAppConf.controlDir, filename),
      new File(getAppConf.styleSheetDir, getAppConf.stylesheetPrefix + filename.substring(0, filename.length - 4) + ".xsl"))
  }

  def main(args: Array[String]): Unit = {
    val filter = new XmlFilenameFilter
    val validator = new TransformationValidator()
    val filenames = getAppConf.inputDir.list(filter)
    val errorCounter = new ErrorCounter
    filenames.map(filename =>
      validator.run(filename, errorCounter)
    )
    if(errorCounter.get > 0) {
      Assert.fail("There were " + errorCounter.get + " validation errors. See log file for more details.")
    }
  }
}

class ErrorCounter {
  var count = 0
  def increment = count += 1
  def get: Int = count
}

class TransformationValidator {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def run(filename: String, errorCounter: ErrorCounter) = {
    val transformConf = TransformationValidator.getTransformConf(filename)
    Assert.assertTrue(transformConf.inputFile.getAbsolutePath + " not found.", transformConf.inputFile.exists)
    Assert.assertTrue(transformConf.controlFile.getAbsolutePath + " not found.", transformConf.controlFile.exists)
    Assert.assertTrue(transformConf.stylesheet.getAbsolutePath + " not found.", transformConf.stylesheet.exists)
    try {
      logger.info("Transforming file [" + transformConf.inputFile.getName + "] with stylesheet [" + transformConf.stylesheet.getName + "]")
      val transformation = new Transform(FileUtils.readFileToString(transformConf.inputFile, "UTF-8"), transformConf.stylesheet)
      runXMLUnit(transformation, transformConf, errorCounter)
    }
    catch {
      case e: IOException => {
        Assert.fail("Transformation failed using " + transformConf.stylesheet.getName)
      }
    }
  }

  def runXMLUnit(transformation: Transform, transformConf: TransformConf, errorCounter: ErrorCounter) = {
    try {
      val resultString: String = transformation.getResultString
      if (TransformationValidator.resultsDirectory != null) {
        FileUtils.writeStringToFile(new File(TransformationValidator.resultsDirectory + transformConf.inputFile.getName), resultString, "UTF-8")
      }
      XMLUnit.setIgnoreWhitespace(TransformationValidator.ignoreWhitespace)
      XMLUnit.setIgnoreAttributeOrder(TransformationValidator.ignoreAttributeOrder)
      val comparison = new DetailedDiff(new Diff(FileUtils.readFileToString(transformConf.controlFile, "UTF-8"), resultString))
      var message: String = "Pass!"
      if (!comparison.similar) {
        message = "File: " + transformConf.inputFile.getName + " Message: " + comparison.getAllDifferences.get(0).toString
      }
      Assert.assertTrue(message, comparison.similar)
    }
    catch {
      case aerr: AssertionError => {
        assertionFailure(aerr.getMessage, transformConf.inputFile, errorCounter)
      }
      case ioex: IOException => {
        failure(ioex.getMessage, transformConf.inputFile)
      }
      case trex: TransformerException => {
        failure(trex.getMessage, transformConf.inputFile)
      }
      case saex: SAXException => {
        failure(saex.getMessage, transformConf.inputFile)
      }
    }
  }

  private def assertionFailure(message: String, file: File, errorCounter: ErrorCounter) {
    logger.error("AssertionError: File: " + file + " Message: " + message)
    errorCounter.increment
  }

  private def failure(message: String, inputFile: File) {

  }

}

class XmlFilenameFilter extends FilenameFilter {

  override def accept(dir: File, name: String): Boolean = {
    if(name.endsWith(".xml")) {
      true
    } else {
      false
    }
  }

}


