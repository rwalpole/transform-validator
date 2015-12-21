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

object TransformationValidator {
  /** Whether to ignore whitespace when comparing the result to the control sample. */
  private val ignoreWhitespace: Boolean = true
  /** Whether to ignore attribute order when comparing the result to the control sample. */
  private val ignoreAttributeOrder: Boolean = true
  /** The name of the directory to store the transformation results in. */
  private val resultsDirectory: String = null

  def getConf: AppConf = {
    val conf = ConfigFactory.load()
    val inputDir = new File(conf.getString("test-conversion.samples.directory.input"))
    val controlDir = new File(conf.getString("test-conversion.samples.directory.control"))
    val stylesheetDir = new File(conf.getString("test-conversion.stylesheet.directory"))
    val stylesheetPrefix = conf.getString("test-conversion.stylesheet.filename.prefix")
    AppConf(inputDir, controlDir, stylesheetDir, stylesheetPrefix)
  }

  def main(args: Array[String]): Unit = {
    val conf = getConf
    val filter = new XmlFilenameFilter
    val validator = new TransformationValidator()
    val filenames = conf.inputDir.list(filter)
    val errorCounter = new ErrorCounter
    filenames.map(filename =>
      validator.run(filename, conf, errorCounter)
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

  def run(filename: String, conf: AppConf, errorCounter: ErrorCounter) = {
    val inputFile = new File(conf.inputDir, filename)
    val controlFile = new File(conf.inputDir, filename)
    val stylesheet = new File(conf.styleSheetDir, conf.stylesheetPrefix + filename.substring(0, filename.length - 4) + ".xsl")
    Assert.assertTrue(inputFile.getAbsolutePath + " not found.", inputFile.exists)
    Assert.assertTrue(controlFile.getAbsolutePath + " not found.", controlFile.exists)
    Assert.assertTrue(stylesheet.getAbsolutePath + " not found.", stylesheet.exists)
    try {
      logger.info("Transforming file [" + inputFile.getName + "] with stylesheet [" + stylesheet.getName + "]")
      val transformation = new Transform(FileUtils.readFileToString(inputFile, "UTF-8"), stylesheet)
      try {
        val resultString: String = transformation.getResultString
        if (TransformationValidator.resultsDirectory != null) {
          FileUtils.writeStringToFile(new File(TransformationValidator.resultsDirectory + inputFile.getName), resultString, "UTF-8")
        }
        XMLUnit.setIgnoreWhitespace(TransformationValidator.ignoreWhitespace)
        XMLUnit.setIgnoreAttributeOrder(TransformationValidator.ignoreAttributeOrder)
        val comparison = new DetailedDiff(new Diff(FileUtils.readFileToString(controlFile, "UTF-8"), resultString))
        var message: String = "Pass!"
        if (!comparison.similar) {
          message = "File: " + inputFile.getName + " Message: " + comparison.getAllDifferences.get(0).toString
        }
        Assert.assertTrue(message, comparison.similar)
      }
      catch {
        case aerr: AssertionError => {
          assertionFailure(aerr.getMessage, inputFile, errorCounter)
        }
        case ioex: IOException => {
          failure(ioex.getMessage, inputFile)
        }
        case trex: TransformerException => {
          failure(trex.getMessage, inputFile)
        }
        case saex: SAXException => {
          failure(saex.getMessage, inputFile)
        }
      }
    }
    catch {
      case e: IOException => {
        Assert.fail("Transformation failed using " + stylesheet.getName)
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
      return true
    } else {
      false
    }
  }

}


