Feature: Schools' onboarding

    In order to register classrooms under a school
    As Lyra
    I want to provide onboarding services for schools

    Scenario: Create school
        Given the school name is "Gloria Fuertes"
        When I click on "Create school"
        Then I receive a confirmation that the school has been successfully created

    Scenario: Cannot create school when the name is longer than 100 characters
        Given the school name is longer than 100 characters
        When I click on "Create school"
        Then I receive the error "size must be between 0 and 100"

    Scenario Outline: Cannot create school when the name <condition>
        Given the school name <condition>
        When I click on "Create school"
        Then I receive the error "must not be blank"

        Examples:
            | condition                |
            | is not provided          |
            | is set to null           |
            | is left blank            |
            | contains only whitespace |
