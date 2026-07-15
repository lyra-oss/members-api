Feature: Parents directory

    In order to look up registered parents
    As Lyra
    I want to list and retrieve parent records

    Background:
        Given parent "Esteban" "Cristóbal" exists with e-mail "esteban.cristobal@example.com"
        And parent "Marta" "Ibáñez" exists with e-mail "marta.ibanez@example.com"
        And parent "Pablo" "Ruiz" exists with e-mail "pablo.ruiz@example.com"
        And I am authenticated with "parents.read" scope

    Scenario: List parents
        When I request the list of parents
        Then the list of parents includes "Esteban" "Cristóbal"

    Scenario: List parents with pagination
        When I request the list of parents with page size 2 and page number 0
        Then I receive a page of 2 parents out of a total of 3

    Scenario: List parents with pagination, second page
        When I request the list of parents with page size 2 and page number 1
        Then I receive a page of 1 parents out of a total of 3

    Scenario: Get a parent
        When I request parent "Esteban" "Cristóbal"
        Then I receive the details of parent "Esteban" "Cristóbal"

    Scenario: Cannot get a parent that does not exist
        When I request a parent that does not exist
        Then I receive a not found error
