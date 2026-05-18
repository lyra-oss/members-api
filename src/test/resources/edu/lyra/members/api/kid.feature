Feature: Kids' onboarding

    In order to let parents register their kids in Lyra
    As Lyra
    I want to provide onboarding services for kids

    Background:
        Given my name is "Esteban"
        And my surname is "Cristóbal"
        And my e-mail address is "esteban.cristobal@example.com"
        And I already have an account

    Scenario: Add a kid to a parent
        Given the kid name is "Alicia"
        And the kid surname is "Cristóbal"
        And the kid birth date is "2019-12-12"
        When I add the kid to my account
        Then I receive a confirmation that the kid has been successfully added
