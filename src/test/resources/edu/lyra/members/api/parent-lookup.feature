Feature: Parents directory

    In order to look up registered parents
    As Lyra
    I want to list and retrieve parent records

    Background:
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
            | Marta   | Ibáñez    | marta.ibanez@example.com      |
            | Pablo   | Ruiz      | pablo.ruiz@example.com        |
            | Lucía   | Moreno    | lucia.moreno@example.com      |
            | Diego   | Torres    | diego.torres@example.com      |
        And I am authenticated with "parents.read" scope

    Scenario: List parents
        When I request the list of parents
        Then the list of parents includes "Esteban" "Cristóbal"

    Scenario: List parents with pagination
        When I request the list of parents with page size 2 and page number 0
        Then I receive a page of 2 parents out of a total of 5

    Scenario: List parents with pagination, second page
        When I request the list of parents with page size 2 and page number 1
        Then I receive a page of 2 parents out of a total of 5

    Scenario: Get a parent
        When I request parent "Esteban" "Cristóbal"
        Then I receive the details of parent "Esteban" "Cristóbal"

    Scenario: Cannot get a parent that does not exist
        When I request a parent that does not exist
        Then I receive a not found error
