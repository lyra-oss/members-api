package com.sagittec.lyra.members.api;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.sagittec.lyra.members.api")
@IncludeClassNamePatterns("com.sagittec.lyra.members.api..*Parent")
class ParentIT {}
