package org.isaqb.asciidoc;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.asciidoctor.Asciidoctor.Factory.create;

public class Main {

  private static final String PROJECT_VERSION = "projectVersion";
  private static final String CURRICULUM_FILE_NAME = "curriculumFileName";
  private static final String VERSION_DATE = "versionDate";
  private static final String LANGUAGES = "languages";
  private static final String[] FORMATS = {"pdf"};

  private static final String LANGUAGE_SEPERATOR = ",";

  private static final String SOURCE_DIR = "./docs/";
  private static final String BASE_DIR = SOURCE_DIR;
  private static final String OUTPUT_DIR = "./build/";

  private static final String ADOC = "adoc";
  private static final String HTML = "html";
  private static final String HTML5 = "html5";
  private static final String PDF = "pdf";
  private static final String ENGLISH = "EN";
  private static final boolean WITH_ANSWERS = true;
  private static final boolean WITHOUT_ANSWERS = false;

  public static void main(final String[] args) {
    Objects.requireNonNull(System.getProperty(PROJECT_VERSION));
    Objects.requireNonNull(System.getProperty(CURRICULUM_FILE_NAME));
    Objects.requireNonNull(System.getProperty(VERSION_DATE));
    Objects.requireNonNull(System.getProperty(LANGUAGES));

    final String projectVersion = System.getProperty(PROJECT_VERSION);
    final String curriculumFileName = System.getProperty(CURRICULUM_FILE_NAME);
    final String versionDate = System.getProperty(VERSION_DATE);
    final String[] languages = System.getProperty(LANGUAGES).split(LANGUAGE_SEPERATOR);

    Stream.of(languages).forEach(language -> convertInLanguage(
        language,
        projectVersion,
        curriculumFileName,
        versionDate
    ));

  }

  private static void convertInLanguage(
      final String language,
      final String projectVersion,
      final String curriculumFileName,
      final String versionDate) {

    final String docTypeQuestion = ENGLISH.equals(language) ? "Question Sheet" : "Fragebogen";
    final String docTypeAnswer = ENGLISH.equals(language) ? "Answer Sheet" : "Antwortbogen";
    convertInFormat(
        PDF, projectVersion,
        curriculumFileName,
        versionDate,
        language,
        docTypeQuestion,
        WITHOUT_ANSWERS
    );

    convertInFormat(
        PDF, projectVersion,
        curriculumFileName,
        versionDate,
        language,
        docTypeAnswer,
        WITH_ANSWERS
    );
  }

  private static void convertInFormat(
      final String format,
      final String projectVersion,
      final String curriculumFileName,
      final String versionDate,
      final String language,
      final String docType,
      final boolean withAnswers) {
    try (final Asciidoctor asciidoctor = create()) {
      final Attributes attributes = toAttributes(
          projectVersion,
          curriculumFileName,
          versionDate,
          language,
          docType,
          withAnswers);
      asciidoctor.convertDirectory(
          List.of(new File("%s%s.%s".formatted(
              SOURCE_DIR,
              curriculumFileName,
              ADOC))),
          Options.builder()
              .baseDir(new File(BASE_DIR))
              .backend(toBackend(format))
              .mkDirs(true)
              .attributes(attributes)
              .standalone(true)
              .toDir(new File(OUTPUT_DIR))
              .safe(SafeMode.UNSAFE)
              .build());

      renameResultAccordingToLanguage(curriculumFileName, format, language, withAnswers);
    }
  }

  private static Attributes toAttributes(
      final String projectVersion,
      final String curriculumFileName,
      final String versionDate,
      final String language,
      final String docType,
      final boolean withAnswers) {
    final String fileVersion = "%s %s-%s".formatted(docType, projectVersion, language);
    final String documentVersion = "%s-%s".formatted(fileVersion, versionDate);

    final Map<String, Object> attributes = new HashMap<>() {{
      put("icons", "font");
      put("version-label", "");
      put("revnumber", fileVersion);
      put("revdate", versionDate);
      put("document-version", documentVersion);
      put("currentDate", versionDate);
      put("language", language);
      put("curriculumFileName", curriculumFileName);
      put("debug_adoc", false);
      put("withAnswers", withAnswers);
      put("pdf-themesdir", "../pdf-theme/themes");
      put("pdf-fontsdir", "../pdf-theme/fonts");
      put("pdf-theme", "isaqb");
      put("stylesheet", "../html-theme/adoc-github.css");
      put("stylesheet-dir", "../html-theme");
      put("data-uri", true);
      put("allow-uri-read", true);
    }};

    return Attributes.builder().attributes(attributes).build();
  }

  private static String toBackend(final String format) {
    return switch (format) {
      case HTML -> HTML5;
      case PDF -> PDF;
      default -> throw new IllegalArgumentException("Unknown target format %s".formatted(format));
    };
  }

  private static void renameResultAccordingToLanguage(
      final String fileName,
      final String format,
      final String language,
      final boolean withAnswers) {
    final File original = new File("%s%s.%s".formatted(OUTPUT_DIR, fileName, format));
    final File renamed = new File(
        "%s%s-%s-%s.%s".formatted(OUTPUT_DIR, fileName, withAnswers ? "answers" : "questions",
            language.toLowerCase(), format));
    if (!original.exists()) {
      System.err.printf("Failed to rename result file %s as it does not exist",
          original.getAbsolutePath());
    } else if (!original.renameTo(renamed)) {
      System.err.printf("Failed to rename result file %s to %s%n", original.getName(),
          renamed.getName());
    }
    original.deleteOnExit();
  }
}
