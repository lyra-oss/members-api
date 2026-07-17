Feature: Teachers' creation

    In order to let teachers use our service
    As Lyra
    I want to provide creation services for teachers

    Background:
        Given I am authenticated with "teachers.create" scope

    Scenario: Create teacher account
        Given the teacher's name is "Marta"
        And the teacher's surname is "Ibáñez"
        And the teacher's e-mail address is "marta.ibanez@example.com"
        And the teacher teaches at school "Gloria Fuertes"
        When I click on "Create teacher account"
        Then I receive a confirmation that the teacher account has been successfully created

    Scenario Outline: Cannot create teacher account when the <field> is too long
        Given the teacher's name is <name>
        And the teacher's surname is <surname>
        And the teacher's e-mail address is "marta.ibanez@example.com"
        And the teacher teaches at school "Gloria Fuertes"
        When I click on "Create teacher account"
        Then I receive an error stating that "<field>" field is incorrect because "size must be between 0 and 100"

        Examples:
            | field          | name                       | surname                    |
            | person.name    | longer than 100 characters | "Ibáñez"                   |
            | person.surname | "Marta"                    | longer than 100 characters |

    Scenario: Cannot create teacher account when the e-mail address is longer than 200 characters
        Given the teacher's name is "Marta"
        And the teacher's surname is "Ibáñez"
        And the teacher's e-mail address is longer than 200 characters
        And the teacher teaches at school "Gloria Fuertes"
        When I click on "Create teacher account"
        Then I receive an error stating that "person.mail" field is incorrect because "size must be between 0 and 200"

    Scenario: Cannot create teacher account when the e-mail address format is invalid
        Given the teacher's name is "Marta"
        And the teacher's surname is "Ibáñez"
        And the teacher's e-mail address is "not-an-email"
        And the teacher teaches at school "Gloria Fuertes"
        When I click on "Create teacher account"
        Then I receive an error stating that "person.mail" field is incorrect because "must be a well-formed email address"

    Scenario Outline: Cannot create teacher account when the name <condition>
        Given the teacher's name <condition>
        And the teacher's surname is "Ibáñez"
        And the teacher's e-mail address is "marta.ibanez@example.com"
        And the teacher teaches at school "Gloria Fuertes"
        When I click on "Create teacher account"
        Then I receive an error stating that "person.name" field is incorrect because "must not be blank"

        Examples:
            | condition                |
            | is not provided          |
            | is set to null           |
            | is left blank            |
            | contains only whitespace |

    Scenario Outline: Cannot create teacher account when the surname <condition>
        Given the teacher's name is "Marta"
        And the teacher's surname <condition>
        And the teacher's e-mail address is "marta.ibanez@example.com"
        And the teacher teaches at school "Gloria Fuertes"
        When I click on "Create teacher account"
        Then I receive an error stating that "person.surname" field is incorrect because "must not be blank"

        Examples:
            | condition                |
            | is not provided          |
            | is set to null           |
            | is left blank            |
            | contains only whitespace |

    Scenario Outline: Cannot create teacher account when the e-mail address <condition>
        Given the teacher's name is "Marta"
        And the teacher's surname is "Ibáñez"
        And the teacher's e-mail address <condition>
        And the teacher teaches at school "Gloria Fuertes"
        When I click on "Create teacher account"
        Then I receive an error stating that "person.mail" field is incorrect because "must not be blank"

        Examples:
            | condition                |
            | is not provided          |
            | is set to null           |
            | is left blank            |
            | contains only whitespace |

    Scenario: Cannot create teacher account when no school is provided
        Given the teacher's name is "Marta"
        And the teacher's surname is "Ibáñez"
        And the teacher's e-mail address is "marta.ibanez@example.com"
        When I click on "Create teacher account"
        Then I receive an error stating that "school" field is incorrect because "must not be null"

    Scenario: Create duplicated teacher account
        Given the teacher's name is "José"
        And the teacher's surname is "García"
        And the teacher's e-mail address is "jose.garcia@example.com"
        And the teacher teaches at school "Gloria Fuertes"
        And the teacher is already registered
        When I click on "Create teacher account"
        Then I receive an error because the account already exists
