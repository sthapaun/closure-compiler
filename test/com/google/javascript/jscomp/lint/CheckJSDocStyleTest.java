/*
 * Copyright 2015 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.javascript.jscomp.lint;

import static com.google.javascript.jscomp.lint.CheckJSDocStyle.CLASS_DISALLOWED_JSDOC;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.CONSTRUCTOR_DISALLOWED_JSDOC;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.EXTERNS_FILES_SHOULD_BE_ANNOTATED;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.INCORRECT_PARAM_NAME;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.INVALID_SUPPRESS;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MISSING_JSDOC;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MISSING_PARAMETER_JSDOC;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MISSING_RETURN_JSDOC;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MIXED_PARAM_JSDOC_STYLES;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MUST_BE_PRIVATE;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MUST_HAVE_TRAILING_UNDERSCORE;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.OPTIONAL_PARAM_NOT_MARKED_OPTIONAL;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.WRONG_NUMBER_OF_PARAMS;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CodingConvention;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.CompilerTestCase;
import com.google.javascript.jscomp.GoogleCodingConvention;

/**
 * Test case for {@link CheckJSDocStyle}.
 */
public final class CheckJSDocStyleTest extends CompilerTestCase {
  public CheckJSDocStyleTest() {
    super("/** @fileoverview\n * @externs\n */");
  }

  private CodingConvention codingConvention;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    codingConvention = new GoogleCodingConvention();
    setAcceptedLanguage(LanguageMode.ECMASCRIPT_NEXT);
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new CheckJSDocStyle(compiler);
  }

  @Override
  protected CompilerOptions getOptions(CompilerOptions options) {
    super.getOptions(options);
    options.setWarningLevel(CheckJSDocStyle.ALL_DIAGNOSTICS, CheckLevel.WARNING);
    return options;
  }

  @Override
  protected CodingConvention getCodingConvention() {
    return codingConvention;
  }

  public void testInvalidSuppress() {
    testSame("/** @suppress {missingRequire} */ var x = new y.Z();");
    testSame("/** @suppress {missingRequire} */ function f() { var x = new y.Z(); }");
    testSame("/** @suppress {missingRequire} */ var f = function() { var x = new y.Z(); }");
    testSame(
        LINE_JOINER.join(
            "var obj = {",
            "  /** @suppress {uselessCode} */",
            "  f: function() {},",
            "}"));
    testSame(
        LINE_JOINER.join(
            "var obj = {",
            "  /** @suppress {uselessCode} */",
            "  f() {},",
            "}"));
    testSame(
        LINE_JOINER.join(
            "class Example {",
            "  /** @suppress {uselessCode} */",
            "  f() {}",
            "}"));
    testSame(
        LINE_JOINER.join(
            "class Example {",
            "  /** @suppress {uselessCode} */",
            "  static f() {}",
            "}"));
    testSame(
        LINE_JOINER.join(
            "class Example {",
            "  /** @suppress {uselessCode} */",
            "  get f() {}",
            "}"));
    testSame(
        LINE_JOINER.join(
            "class Example {",
            "  /**",
            "   * @param {string} val",
            "   * @suppress {uselessCode}",
            "   */",
            "  set f(val) {}",
            "}"));

    testWarning("/** @suppress {uselessCode} */ goog.require('unused.Class');", INVALID_SUPPRESS);
    testSame("/** @suppress {extraRequire} */ goog.require('unused.Class');");
    testSame("/** @const @suppress {duplicate} */ var google = {};");
    testSame("/** @suppress {const} */ var google = {};");
  }

  public void testValidSuppress_withES6Modules01() {
    testSame("export /** @suppress {missingRequire} */ var x = new y.Z();");
  }

  public void testValidSuppress_withES6Modules03() {
    testSame("export /** @const @suppress {duplicate} */ var google = {};");
  }

  public void testExtraneousClassAnnotations() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @constructor",
            " */",
            "var X = class {};"),
        CLASS_DISALLOWED_JSDOC);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @constructor",
            " */",
            "class X {};"),
        CLASS_DISALLOWED_JSDOC);

    // TODO(tbreisacher): Warn for @extends too. We need to distinguish between cases like this
    // which are totally redundant...
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @extends {Y}",
            " */",
            "class X extends Y {};"));

    // ... and ones like this which are not.
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @extends {Y<number>}",
            " */",
            "class X extends Y {};"));

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @implements {Z}",
            " */",
            "class X extends Y {};"));

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @interface",
            " * @extends {Y}",
            " */",
            "class X extends Y {};"));

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @record",
            " * @extends {Y}",
            " */",
            "class X extends Y {};"));
  }

  public void testInvalidExtraneousClassAnnotations_withES6Modules() {
    testWarning(
        LINE_JOINER.join("export", "/**", " * @constructor", " */", "var X = class {};"),
        CLASS_DISALLOWED_JSDOC);
  }

  public void testValidExtraneousClassAnnotations_withES6Modules() {
    testSame(LINE_JOINER.join("export", "/**", " * @extends {Y}", " */", "class X extends Y {};"));
  }

  public void testNestedArrowFunctions() {
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {Object} a",
            " * @return {function(Object): boolean}",
            " */",
            "var haskellStyleEquals = a => b => a == b;"));
  }

  public void testNestedArrowFunctions_withES6Modules() {
    testSame(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {Object} a",
            " * @return {function(Object): boolean}",
            " */",
            "var haskellStyleEquals = a => b => a == b;"));
  }

  public void testGetterSetterMissingJsDoc() {
    testWarning("class Foo { get twentyone() { return 21; } }", MISSING_JSDOC);
    testWarning("class Foo { set someString(s) { this.someString_ = s; } }", MISSING_JSDOC);

    testSame("class Foo { /** @return {number} */ get twentyone() { return 21; } }");
    testSame("class Foo { /** @param {string} s */ set someString(s) { this.someString_ = s; } }");
  }

  public void testGetterSetter_withES6Modules() {
    testSame("export class Foo { /** @return {number} */ get twentyone() { return 21; } }");
  }

  public void testMissingJsDoc() {
    testWarning("function f() {}", MISSING_JSDOC);
    testWarning("var f = function() {}", MISSING_JSDOC);
    testWarning("let f = function() {}", MISSING_JSDOC);
    testWarning("const f = function() {}", MISSING_JSDOC);
    testWarning("foo.bar = function() {}", MISSING_JSDOC);
    testWarning("Foo.prototype.bar = function() {}", MISSING_JSDOC);
    testWarning("class Foo { bar() {} }", MISSING_JSDOC);
    testWarning("class Foo { constructor(x) {} }", MISSING_JSDOC);
    testWarning("var Foo = class { bar() {} };", MISSING_JSDOC);
    testWarning("if (COMPILED) { var f = function() {}; }", MISSING_JSDOC);
    testWarning("var f = async function() {};", MISSING_JSDOC);
    testWarning("async function f() {};", MISSING_JSDOC);
    testWarning("Polymer({ method() {} });", MISSING_JSDOC);
    testWarning("Polymer({ method: function() {} });", MISSING_JSDOC);

    testSame("/** @return {string} */ function f() {}");
    testSame("/** @return {string} */ var f = function() {}");
    testSame("/** @return {string} */ let f = function() {}");
    testSame("/** @return {string} */ const f = function() {}");
    testSame("/** @return {string} */ foo.bar = function() {}");
    testSame("/** @return {string} */ Foo.prototype.bar = function() {}");
    testSame("class Foo { /** @return {string} */ bar() {} }");
    testSame("class Foo { constructor(/** string */ x) {} }");
    testSame("var Foo = class { /** @return {string} */ bar() {} };");
    testSame("/** @param {string} s */ var f = async function(s) {};");
    testSame("/** @param {string} s */ async function f(s) {};");
    testSame("Polymer({ /** @return {null} */ method() {} });");
    testSame("Polymer({ /** @return {null} */ method: function() {} });");
  }

  public void testMissingJsDoc_withES6Modules01() {
    testWarning("export function f() {}", MISSING_JSDOC);
  }

  public void testMissingJsDoc_withES6Modules02() {
    testWarning("export var f = function() {}", MISSING_JSDOC);
  }

  public void testMissingJsDoc_withES6Modules03() {
    testWarning("export let f = function() {}", MISSING_JSDOC);
  }

  public void testMissingJsDoc_withES6Modules04() {
    testWarning("export const f = function() {}", MISSING_JSDOC);
  }

  public void testMissingJsDoc_withES6Modules09() {
    testWarning("export var f = async function() {};", MISSING_JSDOC);
  }

  public void testMissingJsDoc_noWarningIfInlineJsDocIsPresent() {
    testSame("function /** string */ f() {}");
    testSame("function f(/** string */ x) {}");
    testSame("var f = function(/** string */ x) {}");
    testSame("let f = function(/** string */ x) {}");
    testSame("const f = function(/** string */ x) {}");
    testSame("foo.bar = function(/** string */ x) {}");
    testSame("Foo.prototype.bar = function(/** string */ x) {}");
    testSame("class Foo { bar(/** string */ x) {} }");
    testSame("var Foo = class { bar(/** string */ x) {} };");
  }

  public void testMissingJsDoc_noWarningIfInlineJsDocIsPresent_withES6Modules() {
    testSame("export function /** string */ f() {}");
  }

  public void testMissingJsDoc_noWarningIfNotTopLevel() {
    testSame(inIIFE("function f() {}"));
    testSame(inIIFE("var f = function() {}"));
    testSame(inIIFE("let f = function() {}"));
    testSame(inIIFE("const f = function() {}"));
    testSame(inIIFE("foo.bar = function() {}"));
    testSame(inIIFE("class Foo { bar() {} }"));
    testSame(inIIFE("var Foo = class { bar() {} };"));

    testSame("myArray.forEach(function(elem) { alert(elem); });");

    testSame(LINE_JOINER.join(
        "Polymer({",
        "  is: 'example-elem',",
        "  /** @return {null} */",
        "  someMethod: function() {},",
        "});"));

    testSame(LINE_JOINER.join(
        "Polymer({",
        "  is: 'example-elem',",
        "  /** @return {null} */",
        "  someMethod() {},",
        "});"));
  }

  public void testMissingJsDoc_noWarningIfNotTopLevelAndNoParams() {
    testSame(LINE_JOINER.join(
        "describe('a karma test', function() {",
        "  /** @ngInject */",
        "  var helperFunction = function($compile, $rootScope) {};",
        "})"));
  }

  public void testMissingJsDoc_noWarningOnTestFunctions() {
    testSame("function testSomeFunctionality() {}");
    testSame("var testSomeFunctionality = function() {};");
    testSame("let testSomeFunctionality = function() {};");
    testSame("window.testSomeFunctionality = function() {};");
    testSame("const testSomeFunctionality = function() {};");

    testSame("function setUp() {}");
    testSame("function tearDown() {}");
    testSame("var setUp = function() {};");
    testSame("var tearDown = function() {};");
  }

  public void testMissingJsDoc_noWarningOnTestFunctions_withES6Modules() {
    testSame("export function testSomeFunctionality() {}");
  }

  public void testMissingJsDoc_noWarningOnEmptyConstructor() {
    testSame("class Foo { constructor() {} }");
  }

  public void testMissingJsDoc_noWarningOnEmptyConstructor_withES6Modules() {
    testSame("export class Foo { constructor() {} }");
  }

  public void testMissingJsDoc_googModule() {
    testWarning("goog.module('a.b.c'); function f() {}", MISSING_JSDOC);
    testWarning("goog.module('a.b.c'); var f = function() {};", MISSING_JSDOC);
  }

  public void testMissingJsDoc_ES6Module01() {
    testWarning("export default abc; function f() {}", MISSING_JSDOC);
  }

  public void testMissingJsDoc_ES6Module02() {
    testWarning("export default abc; var f = function() {};", MISSING_JSDOC);
  }

  public void testMissingJsDoc_ES6Module03() {
    testWarning("export function f() {};", MISSING_JSDOC);
  }

  public void testMissingJsDoc_ES6Module04() {
    testWarning("export default function () {}", MISSING_JSDOC);
  }

  public void testMissingJsDoc_ES6Module05() {
    testWarning("export default (foo) => { alert(foo); }", MISSING_JSDOC);
  }

  public void testMissingJsDoc_googModule_noWarning() {
    testSame("goog.module('a.b.c'); /** @type {function()} */ function f() {}");
    testSame("goog.module('a.b.c'); /** @type {function()} */ var f = function() {};");
  }

  public void testMissingJsDoc_ES6Module_noWarning01() {
    testSame("export default abc; /** @type {function()} */ function f() {}");
  }

  public void testMissingJsDoc_ES6Module_noWarning02() {
    testSame("export default abc; /** @type {function()} */ var f = function() {};");
  }

  private static String inIIFE(String js) {
    return "(function() {\n" + js + "\n})()";
  }

  public void testMissingParam_noWarning() {
    testSame(LINE_JOINER.join(
        "/**",
        " * @param {string} x",
        " * @param {string} y",
        " */",
        "function f(x, y) {}"));

    testSame(LINE_JOINER.join(
        "/** @override */",
        "Foo.bar = function(x, y) {}"));

    testSame(LINE_JOINER.join(
        "/**",
        " * @param {string=} x",
        " */",
        "function f(x = 1) {}"));

    testSame(LINE_JOINER.join(
        "/**",
        " * @param {number=} x",
        " * @param {number=} y",
        " * @param {number=} z",
        " */",
        "function f(x = 1, y = 2, z = 3) {}"));

    testSame(LINE_JOINER.join(
        "/**",
        " * @param {...string} args",
        " */",
        "function f(...args) {}"));

    testSame(LINE_JOINER.join(
        "(function() {",
        "  myArray.forEach(function(elem) { alert(elem); });",
        "})();"));

    testSame(LINE_JOINER.join(
        "(function() {",
        "  myArray.forEach(elem => alert(elem));",
        "})();"));

    testSame("/** @type {function(number)} */ function f(x) {}");

    testSame("function f(/** string */ inlineArg) {}");
    testSame("/** @export */ function f(/** string */ inlineArg) {}");

    testSame("class Foo { constructor(/** string */ inlineArg) {} }");
    testSame("class Foo { method(/** string */ inlineArg) {} }");

    testSame("/** @export */ class Foo { constructor(/** string */ inlineArg) {} }");
    testSame("class Foo { /** @export */ method(/** string */ inlineArg) {} }");
  }

  public void testMissingParam_noWarning_withES6Modules() {
    testSame("export class Foo { /** @export */ method(/** string */ inlineArg) {} }");
  }

  public void testMissingParam() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} x",
            // No @param for y.
            " */",
            "function f(x, y) {}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} x",
            " */",
            "function f(x = 1) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} x",
            // No @param for y.
            " */",
            "function f(x, y = 1) {}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning("function f(/** string */ x, y) {}", MISSING_PARAMETER_JSDOC);
    testWarning("function f(x, /** string */ y) {}", MISSING_PARAMETER_JSDOC);
    testWarning("function /** string */ f(x) {}", MISSING_PARAMETER_JSDOC);

    testWarning(inIIFE("function f(/** string */ x, y) {}"), MISSING_PARAMETER_JSDOC);
    testWarning(inIIFE("function f(x, /** string */ y) {}"), MISSING_PARAMETER_JSDOC);
    testWarning(inIIFE("function /** string */ f(x) {}"), MISSING_PARAMETER_JSDOC);
  }

  public void testMissingParam_withES6Modules01() {
    testWarning(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {string} x",
            // No @param for y.
            " */",
            "function f(x, y) {}"),
        WRONG_NUMBER_OF_PARAMS);
  }

  public void testMissingParam_withES6Modules02() {
    testWarning(
        LINE_JOINER.join("export", "/**", " * @param {string} x", " */", "function f(x = 1) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);
  }

  public void testMissingParam_withES6Modules03() {
    testWarning("export function f(/** string */ x, y) {}", MISSING_PARAMETER_JSDOC);
  }

  public void testMissingParamWithDestructuringPattern() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, {destructuring:pattern}) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f({destructuring:pattern}, namedParam) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, [pattern]) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f([pattern], namedParam) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {{",
            " *   a: (string|undefined),",
            " *   b: (number|undefined),",
            " *   c: (boolean|undefined)",
            " * }} obj",
            " */",
            "function create({a = 'hello', b = 8, c = false} = {}) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);

    // Same as above except there's an '=' to indicate that it's optional.
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {{",
            " *   a: (string|undefined),",
            " *   b: (number|undefined),",
            " *   c: (boolean|undefined)",
            " * }=} obj",
            " */",
            "function create({a = 'hello', b = 8, c = false} = {}) {}"));
  }

  public void testInvalidMissingParamWithDestructuringPattern_withES6Modules01() {
    testWarning(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, {destructuring:pattern}) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);
  }

  public void testInvalidMissingParamWithDestructuringPattern_withES6Modules02() {
    testWarning(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {{",
            " *   a: (string|undefined),",
            " *   b: (number|undefined),",
            " *   c: (boolean|undefined)",
            " * }} obj",
            " */",
            "function create({a = 'hello', b = 8, c = false} = {}) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);
  }

  public void testValidMissingParamWithDestructuringPattern_withES6Modules() {
    testSame(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {{",
            " *   a: (string|undefined),",
            " *   b: (number|undefined),",
            " *   c: (boolean|undefined)",
            " * }=} obj",
            " */",
            "function create({a = 'hello', b = 8, c = false} = {}) {}"));
  }

  public void testMissingParamWithDestructuringPatternWithDefault() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, {destructuring:pattern} = defaultValue) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, [pattern] = defaultValue) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);
  }

  public void testMissingParamWithDestructuringPatternWithDefault_withES6Modules() {
    testWarning(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, {destructuring:pattern} = defaultValue) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);
  }

  public void testParamWithNoTypeInfo() {
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param x A param with no type information.",
            " */",
            "function f(x) { }"));
  }

  public void testParamWithNoTypeInfo_withES6Modules() {
    testSame(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param x A param with no type information.",
            " */",
            "function f(x) { }"));
  }

  public void testMissingPrivate_noWarningWithClosureConvention() {
    codingConvention = new ClosureCodingConvention();
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @return {number}",
            " * @private",
            " */",
            "X.prototype.foo = function() { return 0; }"));
  }

  public void testMissingPrivate() {
    testWarning(
        LINE_JOINER.join(
            "/** @return {number} */",
            "X.prototype.foo_ = function() { return 0; }"),
        MUST_BE_PRIVATE);

    testWarning(
        LINE_JOINER.join(
            "/** @type {?number} */",
            "X.prototype.foo_ = null;"),
        MUST_BE_PRIVATE);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @return {number}",
            " * @private",
            " */",
            "X.prototype.foo = function() { return 0; }"),
        MUST_HAVE_TRAILING_UNDERSCORE);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @type {number}",
            " * @private",
            " */",
            "X.prototype.foo = 0;"),
        MUST_HAVE_TRAILING_UNDERSCORE);

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @return {number}",
            " * @private",
            " */",
            "X.prototype.foo_ = function() { return 0; }"));

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @type {number}",
            " * @private",
            " */",
            "X.prototype.foo_ = 0;"));

    testSame(
        LINE_JOINER.join(
            "/** @type {number} */",
            "X.prototype['@some_special_property'] = 0;"));
  }

  public void testMissingPrivate_class() {
    testWarning(
        LINE_JOINER.join(
            "class Example {",
            "  /** @return {number} */",
            "  foo_() { return 0; }",
            "}"),
        MUST_BE_PRIVATE);

    testWarning(
        LINE_JOINER.join(
            "class Example {",
            "  /** @return {number} */",
            "  get foo_() { return 0; }",
            "}"),
        MUST_BE_PRIVATE);

    testWarning(
        LINE_JOINER.join(
            "class Example {",
            "  /** @param {number} val */",
            "  set foo_(val) {}",
            "}"),
        MUST_BE_PRIVATE);

    testWarning(
        LINE_JOINER.join(
            "class Example {",
            "  /**",
            "   * @return {number}",
            "   * @private",
            "   */",
            "  foo() { return 0; }",
            "}"),
        MUST_HAVE_TRAILING_UNDERSCORE);

    testWarning(
        LINE_JOINER.join(
            "class Example {",
            "  /**",
            "   * @return {number}",
            "   * @private",
            "   */",
            "  get foo() { return 0; }",
            "}"),
        MUST_HAVE_TRAILING_UNDERSCORE);

    testWarning(
        LINE_JOINER.join(
            "class Example {",
            "  /**",
            "   * @param {number} val",
            "   * @private",
            "   */",
            "  set foo(val) { }",
            "}"),
        MUST_HAVE_TRAILING_UNDERSCORE);
  }

  public void testMissingPrivate_class_withES6Modules01() {
    testWarning(
        LINE_JOINER.join(
            "export class Example {", "  /** @return {number} */", "  foo_() { return 0; }", "}"),
        MUST_BE_PRIVATE);
  }

  public void testMissingPrivate_class_withES6Modules02() {
    testWarning(
        LINE_JOINER.join(
            "export class Example {",
            "  /**",
            "   * @return {number}",
            "   * @private",
            "   */",
            "  foo() { return 0; }",
            "}"),
        MUST_HAVE_TRAILING_UNDERSCORE);
  }

  public void testMissingPrivate_dontWarnOnObjectLiteral() {
    testSame(
        LINE_JOINER.join(
            "var obj = {",
            "  /** @return {number} */",
            "  foo_() { return 0; }",
            "}"));
  }

  public void testMissingPrivate_dontWarnOnObjectLiteral_withES6Modules() {
    testSame(
        LINE_JOINER.join(
            "export var obj = {", "  /** @return {number} */", "  foo_() { return 0; }", "}"));
  }

  public void testOptionalArgs() {
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {number=} n",
            " */",
            "function f(n) {}"));

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {number} opt_n",
            " */",
            "function f(opt_n) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);

    testSame(LINE_JOINER.join(
        "/**",
        " * @param {number=} opt_n",
        " */",
        "function f(opt_n) {}"));
  }

  public void testValidOptionalArgs_withES6Modules() {
    testSame(LINE_JOINER.join("export", "/**", " * @param {number=} n", " */", "function f(n) {}"));
  }

  public void testInvalidOptionalArgs_withES6Modules() {
    testSame(
        LINE_JOINER.join(
            "export", "/**", " * @param {number} opt_n", " */", "function f(opt_n) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);
  }

  public void testParamsOutOfOrder() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {?} second",
            " * @param {?} first",
            " */",
            "function f(first, second) {}"),
        INCORRECT_PARAM_NAME);
  }

  public void testParamsOutOfOrder_withES6Modules() {
    testWarning(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {?} second",
            " * @param {?} first",
            " */",
            "function f(first, second) {}"),
        INCORRECT_PARAM_NAME);
  }

  public void testMixedStyles() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {?} first",
            " * @param {string} second",
            " */",
            "function f(first, /** string */ second) {}"),
        MIXED_PARAM_JSDOC_STYLES);
  }

  public void testMixedStyles_withES6Modules() {
    testWarning(
        LINE_JOINER.join(
            "export",
            "/**",
            " * @param {?} first",
            " * @param {string} second",
            " */",
            "function f(first, /** string */ second) {}"),
        MIXED_PARAM_JSDOC_STYLES);
  }

  public void testDestructuring() {
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {{x: number, y: number}} point",
            " */",
            "function getDistanceFromZero({x, y}) {}"));

    testSame("function getDistanceFromZero(/** {x: number, y: number} */ {x, y}) {}");
  }

  public void testDestructuring_withES6Modules() {
    testSame("export function getDistanceFromZero(/** {x: number, y: number} */ {x, y}) {}");
  }

  public void testMissingReturn_functionStatement_noWarning() {
    testSame("/** @param {number} x */ function f(x) {}");
    testSame("/** @constructor */ function f() {}");
    testSame("/** @param {number} x */ function f(x) { function bar() { return x; } }");
    testSame("/** @param {number} x */ function f(x) { return; }");
    testSame("/** @param {number} x @return {number} */ function f(x) { return x; }");
    testSame("/** @param {number} x */ function /** number */ f(x) { return x; }");
    testSame("/** @inheritDoc */ function f(x) { return x; }");
    testSame("/** @override */ function f(x) { return x; }");
  }

  public void testMissingReturn_functionStatement_noWarning_withES6Modules() {
    testSame("export /** @param {number} x */ function f(x) {}");
  }

  public void testMissingReturn_assign_noWarning() {
    testSame("/** @param {number} x */ f = function(x) {}");
    testSame("/** @constructor */ f = function() {}");
    testSame("/** @param {number} x */ f = function(x) { function bar() { return x; } }");
    testSame("/** @param {number} x */ f = function(x) { return; }");
    testSame("/** @param {number} x @return {number} */ f = function(x) { return x; }");
    testSame("/** @inheritDoc */ f = function(x) { return x; }");
    testSame("/** @override */ f = function(x) { return x; }");
  }

  public void testMissingReturn_var_noWarning() {
    testSame("/** @param {number} x */ var f = function(x) {}");
    testSame("/** @constructor */ var f = function() {}");
    testSame("/** @param {number} x */ var f = function(x) { function bar() { return x; } }");
    testSame("/** @param {number} x */ var f = function(x) { return; }");
    testSame("/** @param {number} x @return {number} */ var f = function(x) { return x; }");
    testSame("/** @const {function(number): number} */ var f = function(x) { return x; }");
    testSame("/** @inheritDoc */ var f = function(x) { return x; }");
    testSame("/** @override */ var f = function(x) { return x; }");
  }

  public void testMissingReturn_var_noWarning_withES6Modules() {
    testSame("export /** @param {number} x */ var f = function(x) {}");
  }

  public void testMissingReturn_functionStatement() {
    testWarning("/** @param {number} x */ function f(x) { return x; }", MISSING_RETURN_JSDOC);
    testWarning(
        LINE_JOINER.join(
            "/** @param {number} x */",
            "function f(x) {",
            "  /** @param {number} x */",
            "  function bar(x) {",
            "    return x;",
            "  }",
            "}"),
        MISSING_RETURN_JSDOC);
    testWarning(
        "/** @param {number} x */ function f(x) { if (true) { return x; } }", MISSING_RETURN_JSDOC);
    testWarning(
        "/** @param {number} x @constructor */ function f(x) { return x; }", MISSING_RETURN_JSDOC);
  }

  public void testMissingReturn_functionStatement_withES6Modules() {
    testWarning(
        "export /** @param {number} x */ function f(x) { return x; }", MISSING_RETURN_JSDOC);
  }

  public void testMissingReturn_assign() {
    testWarning("/** @param {number} x */ f = function(x) { return x; }", MISSING_RETURN_JSDOC);
    testWarning(
        LINE_JOINER.join(
            "/** @param {number} x */",
            "function f(x) {",
            "  /** @param {number} x */",
            "  bar = function(x) {",
            "    return x;",
            "  }",
            "}"),
        MISSING_RETURN_JSDOC);
    testWarning(
        "/** @param {number} x */ f = function(x) { if (true) { return x; } }",
        MISSING_RETURN_JSDOC);
    testWarning(
        "/** @param {number} x @constructor */ f = function(x) { return x; }",
        MISSING_RETURN_JSDOC);
  }

  public void testMissingReturn_assign_withES6Modules() {
    testWarning(
        LINE_JOINER.join(
            "/** @param {number} x */",
            "export",
            "function f(x) {",
            "  /** @param {number} x */",
            "  bar = function(x) {",
            "    return x;",
            "  }",
            "}"),
        MISSING_RETURN_JSDOC);
  }

  public void testMissingReturn_var() {
    testWarning("/** @param {number} x */ var f = function(x) { return x; }", MISSING_RETURN_JSDOC);
    testWarning(
        LINE_JOINER.join(
            "/** @param {number} x */",
            "function f(x) {",
            "  /** @param {number} x */",
            "  var bar = function(x) {",
            "    return x;",
            "  }",
            "}"),
        MISSING_RETURN_JSDOC);
    testWarning(
        "/** @param {number} x */ var f = function(x) { if (true) { return x; } }",
        MISSING_RETURN_JSDOC);
    testWarning(
        "/** @param {number} x @constructor */ var f = function(x) { return x; }",
        MISSING_RETURN_JSDOC);
  }

  public void testMissingReturn_var_withES6Modules() {
    testWarning(
        "export /** @param {number} x */ var f = function(x) { return x; }", MISSING_RETURN_JSDOC);
  }

  public void testExternsAnnotation() {
    testSame(
        "function Example() {}",
        "",
        EXTERNS_FILES_SHOULD_BE_ANNOTATED);

    testSame(
        "/** @fileoverview Some super cool externs.\n * @externs\n */ function Example() {}",
        "");

    testSame(
        LINE_JOINER.join(
            "/** @fileoverview Some super cool externs.\n * @externs\n */",
            "/** @constructor */ function Example() {}",
            "/** @param {number} x */ function example2(x) {}"),
        "");

    test(
        new String[] {
            "/** @fileoverview Some externs.\n * @externs\n */ /** @const */ var example;",
            "/** @fileoverview Some more.\n * @externs\n */ /** @const */ var example2;",
        },
        new String[] {});
  }

  public void testInvalidExternsAnnotation_withES6Modules() {
    testSame("export function Example() {}", "", EXTERNS_FILES_SHOULD_BE_ANNOTATED);
  }

  public void testValidExternsAnnotation_withES6Modules() {
    testSame(
        "export /** @fileoverview Some super cool externs.\n * @externs\n */ function Example() {}",
        "");
  }

  public void testConstructorsDontHaveVisibility() {
    testSame(inIIFE("/** @private */ class Foo { constructor() {} }"));

    testWarning(
        inIIFE("class Foo { /** @private */ constructor() {} }"), CONSTRUCTOR_DISALLOWED_JSDOC);
  }
}
