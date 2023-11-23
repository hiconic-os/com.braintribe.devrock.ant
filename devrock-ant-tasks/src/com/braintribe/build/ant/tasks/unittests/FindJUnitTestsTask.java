// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.unittests;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.common.RegexCheck;
import com.braintribe.common.uncheckedcounterpartexceptions.UncheckedClassNotFoundException;
import com.braintribe.logging.Logger;
import com.braintribe.utils.ChildFirstClassLoader;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.CommonTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.ReflectionTools;
import com.braintribe.utils.StringTools;

/**
 * Finds java source files in the specified directory (and its subdirectories) and searches for junit test methods.
 * Methods can be filtered by any annotation (configurable via regex) and junit categories (configurable super type of
 * excluded categories). Purpose of this method is to avoid having to define test suites, but supporting test exclusion
 * via (custom) annotations and categories nonetheless.
 *
 * @author michael.lafite
 */
public class FindJUnitTestsTask extends Task {

	private static Logger logger = Logger.getLogger(FindJUnitTestsTask.class);

	// accessing annotations via reflection to avoid having to add full junit dependency
	private static final String JUNIT_ANNOTATIONS_TEST = "org.junit.Test";
	private static final String JUNIT_ANNOTATIONS_IGNORE = "org.junit.Ignore";
	private static final String JUNIT_ANNOTATIONS_CATEGORY = "org.junit.experimental.categories.Category";
	private static final String JUNIT_ANNOTATIONS_CATEGORY_CATEGORIES_GETTER_METHOD_NAME = "value";
	private static final String JUNIT_ANNOTATIONS_RUNWITH = "org.junit.runner.RunWith";
	private static final String JUNIT_ANNOTATIONS_RUNWITH_RUNNER_GETTER_METHOD_NAME = "value";
	private static final String JUNIT_RUNNERS_PARAMETERIZED = "org.junit.runners.Parameterized";

	private static final String BT_CATEGORIES_KNOWNISSUE = "com.braintribe.testing.category.KnownIssue";
	private static final String BT_CATEGORIES_SLOW = "com.braintribe.testing.category.Slow";
	private static final String BT_CATEGORIES_SPECIALENVIRONMENT = "com.braintribe.testing.category.SpecialEnvironment";
	
	private static final String BT_CATEGORIES_KNOWNISSUE__OLD = "com.braintribe.utils.test.category.KnownIssue";
	private static final String BT_CATEGORIES_SLOW__OLD = "com.braintribe.utils.test.category.Slow";
	private static final String BT_CATEGORIES_SPECIALENVIRONMENT__OLD = "com.braintribe.utils.test.category.SpecialEnvironment";
	
	static final String DUMMYMETHODNAME_FOR_ALLMETHODS = "[ALL_METHODS]";
	static final String DEFAULT_TEST_CLASSES_SEPARATOR = "^";
	static final String DEFAULT_CLASS_METHODS_SEPARATOR = ":";

	/**
	 * This is used as a work-around for some properties, because passing properties ONLY if they are set is
	 * tricky/inconvenient in Ant.
	 */
	public static final String NOT_SET = "[NOT_SET]";

	private File testsSrcDir;
	private String testMethodsSeparator = ",";
	private String testClassAndMethodsSeparator = ":";
	private String testClassesSeparator = "^";
	private String annotationsExcludeRegex = JUNIT_ANNOTATIONS_IGNORE;
	private String excludedCategoriesSeparator = ",";
	private String excludedCategories = 
			 BT_CATEGORIES_KNOWNISSUE + this.excludedCategoriesSeparator + BT_CATEGORIES_SLOW
				+ this.excludedCategoriesSeparator + BT_CATEGORIES_SPECIALENVIRONMENT + this.excludedCategoriesSeparator
			+ BT_CATEGORIES_KNOWNISSUE__OLD + this.excludedCategoriesSeparator + BT_CATEGORIES_SLOW__OLD
			+ this.excludedCategoriesSeparator + BT_CATEGORIES_SPECIALENVIRONMENT__OLD;
	private List<Class<?>> excludedCategoryClasses;
	private String resultProperty = "testClassesAndMethods";
	private String testsClasspathString;
	private String testsClasspathStringPathSeparator = System.getProperty("path.separator");
	private ClassLoader testsClassLoader;
	private String testClassesIncludeRegex = ".*";
	private String testClassesExcludeRegex = "";
	private RegexCheck testClassesRegexCheck;
	private boolean verbose;

	/**
	 * Returns <code>true</code>, if the <code>propertyValue</code> is not <code>null</code> and not {@value #NOT_SET}.
	 */
	private static boolean isSetPropertyValue(String propertyValue) {
		if (propertyValue != null && !propertyValue.equals(NOT_SET)) {
			return true;
		}
		return false;
	}

	public boolean isVerbose() {
		return this.verbose;
	}

	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public String getTestClassesIncludeRegex() {
		return this.testClassesIncludeRegex;
	}

	public void setTestClassesIncludeRegex(final String testClassesIncludeRegex) {
		if (isSetPropertyValue(testClassesIncludeRegex)) {
			this.testClassesIncludeRegex = testClassesIncludeRegex;
		}
	}

	public String getTestClassesExcludeRegex() {
		return this.testClassesExcludeRegex;
	}

	public void setTestClassesExcludeRegex(final String testClassesExcludeRegex) {
		if (isSetPropertyValue(testClassesExcludeRegex)) {
			this.testClassesExcludeRegex = testClassesExcludeRegex;
		}
	}

	public File getTestsSrcDir() {
		return this.testsSrcDir;
	}

	public void setTestsSrcDir(final File testsSrcDir) {
		this.testsSrcDir = testsSrcDir;
	}

	public String getTestMethodsSeparator() {
		return this.testMethodsSeparator;
	}

	public void setTestMethodsSeparator(final String testMethodsSeparator) {
		if (isSetPropertyValue(testMethodsSeparator)) {
			this.testMethodsSeparator = testMethodsSeparator;
		}
	}

	public String getTestClassAndMethodsSeparator() {
		return this.testClassAndMethodsSeparator;
	}

	public void setTestClassAndMethodsSeparator(final String testClassAndMethodsSeparator) {
		if (isSetPropertyValue(testClassAndMethodsSeparator)) {
			this.testClassAndMethodsSeparator = testClassAndMethodsSeparator;
		}
	}

	public String getTestClassesSeparator() {
		return this.testClassesSeparator;
	}

	public void setTestClassesSeparator(final String testClassesSeparator) {
		if (isSetPropertyValue(testClassesSeparator)) {
			this.testClassesSeparator = testClassesSeparator;
		}
	}

	public String getAnnotationsExcludeRegex() {
		return this.annotationsExcludeRegex;
	}

	public void setAnnotationsExcludeRegex(final String annotationsExcludeRegex) {
		if (isSetPropertyValue(annotationsExcludeRegex)) {
			this.annotationsExcludeRegex = annotationsExcludeRegex;
		}
	}

	public String getExcludedCategories() {
		return this.excludedCategories;
	}

	public void setExcludedCategories(final String excludedCategories) {
		if (isSetPropertyValue(excludedCategories)) {
			this.excludedCategories = excludedCategories;
		}
	}

	public String getExcludedCategoriesSeparator() {
		return this.excludedCategoriesSeparator;
	}

	public void setExcludedCategoriesSeparator(final String excludedCategoriesSeparator) {
		if (isSetPropertyValue(excludedCategoriesSeparator)) {
			this.excludedCategoriesSeparator = excludedCategoriesSeparator;
		}
	}

	public String getResultProperty() {
		return this.resultProperty;
	}

	public void setResultProperty(final String resultProperty) {
		if (isSetPropertyValue(resultProperty)) {
			this.resultProperty = resultProperty;
		}
	}

	public String getTestsClasspathString() {
		return this.testsClasspathString;
	}

	public void setTestsClasspathString(final String testsClasspathString) {
		if (isSetPropertyValue(testsClasspathString)) {
			this.testsClasspathString = testsClasspathString;
		}
	}

	public String getTestsClasspathStringPathSeparator() {
		return testsClasspathStringPathSeparator;
	}

	public void setTestsClasspathStringPathSeparator(String testsClasspathStringPathSeparator) {
		if (isSetPropertyValue(testsClasspathStringPathSeparator)) {
			this.testsClasspathStringPathSeparator = testsClasspathStringPathSeparator;
		}
	}

	private RegexCheck getTestClassesRegexCheck() {
		if (this.testClassesRegexCheck == null) {
			this.testClassesRegexCheck = new RegexCheck(getTestClassesIncludeRegex(), getTestClassesExcludeRegex());
		}
		return this.testClassesRegexCheck;
	}

	private ClassLoader getTestsClassLoader() {
		if (this.testsClassLoader == null) {

			final ClassLoader parent = Thread.currentThread().getContextClassLoader();

			if (CommonTools.isEmpty(getTestsClasspathString())) {
				// no classpath specified --> just using parent class loader
				this.testsClassLoader = parent;
			} else {
				this.testsClassLoader = createClassloader(getTestsClasspathString(),
						getTestsClasspathStringPathSeparator(), parent);
			}

			// do a short check to find major classpath misconfigurations immediately
			getExistingClass(JUNIT_ANNOTATIONS_TEST);
		}
		return this.testsClassLoader;
	}

	/**
	 * Creates a class loader that can load the classes specified in the passed <code>classpathString</code> (a
	 * <code>;</code> separated list of file paths).
	 */
	static ClassLoader createClassloader(final String classpathString, final String classpathStringPathSeparator,
			final ClassLoader parent) {
		final List<String> filePaths = CollectionTools.decodeCollection(classpathString, classpathStringPathSeparator,
				false, true, true, false);
		final List<URL> urls = new ArrayList<URL>();
		for (final String filePath : filePaths) {
			final File file = new File(filePath);
			if (!file.exists()) {
				throw new IllegalArgumentException(
						"File " + file + " doesn't exist! Invalid classpathString? (" + classpathString + ")");
			}
			final URL url = FileTools.toURL(file);
			urls.add(url);
		}

		final URL[] urlArray = urls.toArray(new URL[urls.size()]);
		return new ChildFirstClassLoader(urlArray, parent);
	}

	private List<Class<?>> getExcludedCategoryClasses() {
		if (this.excludedCategoryClasses == null) {
			this.excludedCategoryClasses = new ArrayList<Class<?>>();
			if (!CommonTools.isEmpty(getExcludedCategories())) {
				for (final String excludedCategory : CollectionTools.decodeCollection(getExcludedCategories(),
						getExcludedCategoriesSeparator(), false, true, true, false)) {
					final Class<?> excludedCategoryClass = getOptionalClass(excludedCategory);
					if (excludedCategoryClass != null) {
						this.excludedCategoryClasses.add(excludedCategoryClass);
					} else {
						/*
						 * the category class doesn't exist. This may happen when testing multiple artifacts with the
						 * same set of excluded categories but different class paths. If a test artifact directly
						 * depends on JUnit (instead of one of BT test artifacts where the BT annotations are part of
						 * the dependencies) the BT categories may not be available on the classpath. This is not an
						 * issue though, because those categories then also cannot be set in the test classes and thus
						 * we don't have to search for them.
						 */
					}
				}
			}
		}
		return this.excludedCategoryClasses;
	}

	/**
	 * Stores the result of {@link #getTestClassesAndMethodsString()} in the {@link #getResultProperty() result
	 * property}.
	 */
	@Override
	public void execute() throws BuildException {

		final String result = getTestClassesAndMethodsString();

		getProject().setProperty(getResultProperty(), result);
	}

	/**
	 * Finds all test classes and methods (that are not filtered out) and returns concatenated string. Example:<br/>
	 * <code>
	 * com.braintribe.test.unittesting.AnotherTestedClassTest:test^com.braintribe.test.unittesting.TestedClassTest:testAssertionFails,testUnexpectedException,testSuccess
	 * </code>
	 */
	public String getTestClassesAndMethodsString() throws BuildException {

		if (this.verbose) {
			logger.info(FindJUnitTestsTask.class.getSimpleName() + " parameters: "
					+ CommonTools.getParametersStringWithoutParentheses("testsSrcDir", testsSrcDir,
							"testMethodsSeparator", testMethodsSeparator, "testClassAndMethodsSeparator",
							testClassAndMethodsSeparator, "testClassesSeparator", testClassesSeparator,
							"annotationsExcludeRegex", annotationsExcludeRegex, "excludedCategories",
							excludedCategories, "excludedCategoriesSeparator", excludedCategoriesSeparator,
							"resultProperty", resultProperty, "testClassesIncludeRegex", testClassesIncludeRegex,
							"testClassesExcludeRegex", testClassesExcludeRegex));
		}

		if (getTestsSrcDir() == null) {
			throw new BuildException("No sources directory specified!");
		}

		if (!getTestsSrcDir().exists()) {
			throw new BuildException(
					"The specified sources directory " + getTestsSrcDir().getAbsolutePath() + " doesn't exist!");
		}

		if (logger.isInfoEnabled()) {
			logger.info("Searching for tests in " + getTestsSrcDir().getAbsolutePath() + " ...");
		}

		final List<TestClass> testClasses = new ArrayList<TestClass>();
		findTestClasses(getTestsSrcDir(), "", testClasses);

		if (logger.isInfoEnabled()) {
			if (testClasses.isEmpty()) {
				logger.info("Found no tests!");
			} else {
				logger.info("Found the following tests:\n" + StringTools.createStringFromCollection(testClasses, "\n"));
			}
		}

		final String result = StringTools.createStringFromCollection(testClasses, getTestClassesSeparator());

		if (isVerbose() && logger.isDebugEnabled()) {
			logger.debug("Result: " + result);
		}

		return result;
	}

	private class TestClass {
		private String testClass;
		private List<String> testMethods;

		@Override
		public String toString() {
			final String methodNames = StringTools.createStringFromCollection(this.testMethods,
					getTestMethodsSeparator());
			return this.testClass + getTestClassAndMethodsSeparator() + methodNames;
		}
	}

	private void findTestClasses(final File dir, final String packageName, final List<TestClass> testClasses) {
		for (final File file : dir.listFiles()) {
			if (file.isDirectory()) {
				final String subpackageName = (packageName == "") ? file.getName()
						: (packageName + "." + file.getName());
				findTestClasses(file, subpackageName, testClasses);
				continue;
			}

			final String javaExtension = ".java";
			final int lengthOfJavaExtension = javaExtension.length();

			if (!file.getName().endsWith(javaExtension)) {
				continue;
			}

			final String simpleClassName = StringTools.removeLastNCharacters(file.getName(), lengthOfJavaExtension);
			final String className = (packageName == "") ? simpleClassName : packageName + "." + simpleClassName;

			if (!getTestClassesRegexCheck().check(className)) {
				if (isVerbose() && logger.isInfoEnabled()) {
					logger.info(className + " excluded by regex!");
				}
				continue;
			}

			final Class<?> clazz = getExistingClass(className);

			// exclude abstract classes (since they cannot be instantiated)
			if (ReflectionTools.isAbstract(clazz)) {
				continue;
			}

			if (isExcludedByAnnotation(clazz)) {
				continue;
			}

			final TestClass testClass = getTestClass(clazz);
			if (testClass != null) {
				testClasses.add(testClass);
			}
		}
	}

	private TestClass getTestClass(final Class<?> clazz) {

		final List<String> testMethodNames = new ArrayList<String>();

		final Method[] allMethods;
		try {
			allMethods = clazz.getMethods();
		} catch (final Throwable t) {
			/*
			 * shouldn't happen, but there used to be some issues. Therefore we want a meaningful error message, if it
			 * happens again.
			 */
			// throwing RuntimeException instead of BuildException, becaue Ant wouldn't log the cause
			throw new RuntimeException("Error while getting methods of clazz " + clazz.getName() + "!", t);
		}

		// for each public method (junit test methods must be public) in this class or one of its super types ...
		for (final Method method : allMethods) {
			if (getAnnotationsByTypeName(ReflectionTools.getAnnotations(method), JUNIT_ANNOTATIONS_TEST).isEmpty()) {
				// not a junit test method
				continue;
			}

			if (isExcludedByAnnotation(method)) {
				continue;
			}

			if (isVerbose() && logger.isDebugEnabled()) {
				logger.debug(method + " included!");
			}
			testMethodNames.add(method.getName());
		}

		if (testMethodNames.isEmpty()) {
			if (clazz.getName().endsWith("Test") && isVerbose() && logger.isInfoEnabled()) {
				logger.info("Class " + clazz.getName() + " excluded, because not a single test method was included!");
			}
			return null;
		}

		// ***** Special handling for RunWith annotations *********************
		Annotation runWithAnnotation = CollectionTools.getFirstElementOrNull(CollectionTools.checkSize(
				getAnnotationsByTypeName(ReflectionTools.getAnnotations(clazz), JUNIT_ANNOTATIONS_RUNWITH), 0, 1));
		if (runWithAnnotation != null) {
			final Method runnerGetterMethod = ReflectionTools
					.getMethod(JUNIT_ANNOTATIONS_RUNWITH_RUNNER_GETTER_METHOD_NAME, runWithAnnotation.getClass());
			Class<?> runner;
			try {
				runner = (Class<?>) runnerGetterMethod.invoke(runWithAnnotation);
			} catch (final Exception e) {
				throw new BuildException("Error while getting runner from RunWith annotation!", e);
			}

			/**
			 * [ALL_METHODS] is used to indicate that all methods must be run. This is used for parameterized tests
			 * where specifying the test method names doesn't work with junit-Ant-task (version 1.9.7).
			 */
			logger.info("Found custom test runner " + clazz.getName() + ". Setting dummy method name.");

			testMethodNames.clear();
			testMethodNames.add(DUMMYMETHODNAME_FOR_ALLMETHODS);
		}
		// ********************************************************************

		TestClass testClass = null;
		testClass = new TestClass();
		testClass.testClass = clazz.getName();
		testClass.testMethods = testMethodNames;
		return testClass;
	}

	private boolean isExcludedByAnnotation(final Object annotatable) {
		if (annotatable instanceof Class) {
			// TODO: add a test for exclusion in super class/interface
		
			Class<?> clazz = (Class<?>) annotatable;
			@SuppressWarnings("rawtypes")
			List<Class> superTypes = new ArrayList<>();
			if (clazz.getSuperclass() != null) {
				superTypes.add(clazz.getSuperclass());
			}
			if(clazz.getInterfaces() != null) {
				superTypes.addAll(Arrays.asList(clazz.getInterfaces()));
			}
			for (Class<?> superType : superTypes) {
				if (isExcludedByAnnotation(superType)) {
					return true;
				}
			}
		}
		
		final List<Annotation> annotations = ReflectionTools.getAnnotations(annotatable);
		if (annotations.isEmpty()) {
			return false;
		}

		final List<Annotation> excludedAnnotations = ReflectionTools.getAnnotationsByTypeName(annotations,
				new RegexCheck(getAnnotationsExcludeRegex()));
		if (!excludedAnnotations.isEmpty()) {
			if (isVerbose() && logger.isInfoEnabled()) {
				logger.info(annotatable + " excluded by annotation(s): " + excludedAnnotations);
			}
			return true;
		}

		final List<? extends Annotation> categoryAnnotations = getAnnotationsByTypeName(annotations,
				JUNIT_ANNOTATIONS_CATEGORY);

		for (final Annotation categoryAnnotation : categoryAnnotations) {
			final Method categoriesGetterMethod = ReflectionTools
					.getMethod(JUNIT_ANNOTATIONS_CATEGORY_CATEGORIES_GETTER_METHOD_NAME, categoryAnnotation.getClass());
			Class<?>[] categories;
			try {
				categories = (Class<?>[]) categoriesGetterMethod.invoke(categoryAnnotation);
			} catch (final Exception e) {
				throw new BuildException("Error while getting categories from category annotation!", e);
			}
			for (final Class<?> category : categories) {
				for (final Class<?> excludedCategory : getExcludedCategoryClasses()) {
					if (excludedCategory.isAssignableFrom(category)) {
						if (isVerbose() && logger.isInfoEnabled()) {
							logger.info("  " + annotatable + " excluded by category " + category.getSimpleName() + ".");
						}
						return true;
					}
				}
			}
		}

		return false;
	}

	private List<Annotation> getAnnotationsByTypeName(final Iterable<Annotation> annotations,
			final String annotationTypeName) {
		@SuppressWarnings("unchecked")
		final Class<? extends Annotation> annotationType = (Class<? extends Annotation>) getExistingClass(
				annotationTypeName);
		final List<Annotation> result = ReflectionTools.getAnnotationsByType(annotations, annotationType);
		return result;
	}

	private Class<?> getOptionalClass(final String className) {
		try {
			return Class.forName(className, false, getTestsClassLoader());
		} catch (final ClassNotFoundException e) {
			return null;
		}
	}

	private Class<?> getExistingClass(final String className) {
		try {
			return Class.forName(className, false, getTestsClassLoader());
		} catch (final ClassNotFoundException e) {
			throw new UncheckedClassNotFoundException("Required class " + className
					+ " not available! Maybe classpath was not properly configured? Classpath:\n"
					+ getTestsClasspathString());
		}
	}
}
