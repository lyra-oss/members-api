Feature: Parents' onboarding

    In order to let parents use our service
    As Lyra
    I want to provide onboarding services

    Scenario: Create parent account
        Given my name is "Esteban"
        And my surname is "Cristóbal"
        And my e-mail address is "esteban.cristobal@example.com"
        When I click on "Create account"
        Then I receive a confirmation that my account has been successfully created

    Scenario Outline: Cannot create account when the <field> is too long
        Given my name is <name>
        And my surname is <surname>
        And my e-mail address is "esteban.cristobal@example.com"
        When I click on "Create account"
        Then I receive the error "size must be between 0 and 100"

        Examples:
            | field   | name                       | surname                    |
            | name    | longer than 100 characters | "Cristóbal"                |
            | surname | "Esteban"                  | longer than 100 characters |

    Scenario: Cannot create account when the e-mail address is longer than 200 characters
        Given my name is "Esteban"
        And my surname is "Cristóbal"
        And my e-mail address is longer than 200 characters
        When I click on "Create account"
        Then I receive the error "size must be between 0 and 200"

    Scenario: Cannot create account when the e-mail address format is invalid
        Given my name is "Esteban"
        And my surname is "Cristóbal"
        And my e-mail address is "not-an-email"
        When I click on "Create account"
        Then I receive the error "must be a well-formed email address"

    Scenario Outline: Cannot create account when the name <condition>
        Given my name <condition>
        And my surname is "Cristóbal"
        And my e-mail address is "esteban.cristobal@example.com"
        When I click on "Create account"
        Then I receive the error "must not be blank"

        Examples:
            | condition                |
            | is not provided          |
            | is set to null           |
            | is left blank            |
            | contains only whitespace |

    Scenario Outline: Cannot create account when the surname <condition>
        Given my name is "Esteban"
        And my surname <condition>
        And my e-mail address is "esteban.cristobal@example.com"
        When I click on "Create account"
        Then I receive the error "must not be blank"

        Examples:
            | condition                |
            | is not provided          |
            | is set to null           |
            | is left blank            |
            | contains only whitespace |

    Scenario Outline: Cannot create account when the e-mail address <condition>
        Given my name is "Esteban"
        And my surname is "Cristóbal"
        And my e-mail address <condition>
        When I click on "Create account"
        Then I receive the error "must not be blank"

        Examples:
            | condition                |
            | is not provided          |
            | is set to null           |
            | is left blank            |
            | contains only whitespace |

    Scenario: Create duplicated parent account
        Given my name is "José"
        And my surname is "García"
        And my e-mail address is "jose.garcia@example.com"
        And I already have an account
        When I click on "Create account"
        Then I receive an error because the account already exists
