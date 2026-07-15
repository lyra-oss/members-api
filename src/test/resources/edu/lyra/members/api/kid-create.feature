Feature: Kids' creation

    In order to let parents register their kids in Lyra
    As Lyra
    I want to provide creation services for kids

    Background:
        Given my name is "Esteban"
        And my surname is "Cristóbal"
        And my e-mail address is "esteban.cristobal@example.com"
        And I already have an account
        And I am authenticated as "esteban.cristobal@example.com" with "kids.create" scope

    Scenario: Add a kid to a parent
        Given the kid name is "Alicia"
        And the kid surname is "Cristóbal"
        And the kid birth date is "2019-12-12"
        When I add the kid to my account
        Then I receive a confirmation that the kid has been successfully added

    Scenario: Cannot add a kid when not authenticated as a registered parent
        Given I am authenticated with "kids.create" scope
        And the kid name is "Alicia"
        And the kid surname is "Cristóbal"
        And the kid birth date is "2019-12-12"
        When I add the kid to my account
        Then I receive a forbidden error
