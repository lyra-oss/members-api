Feature: Kids directory

    In order to look up registered kids
    As Lyra
    I want to list and retrieve kid records

    Background:
        Given a kid named "Alicia" "Cristóbal" born on "2019-12-12" exists
        And a kid named "Marta" "Ibáñez" born on "2018-05-20" exists
        And a kid named "Pablo" "Ruiz" born on "2020-03-08" exists
        And I am authenticated with "kids.read" scope

    Scenario: List kids
        When I request the list of kids
        Then the list of kids includes "Alicia" "Cristóbal"

    Scenario: List kids with pagination
        When I request the list of kids with page size 2 and page number 0
        Then I receive a page of 2 kids out of a total of 3

    Scenario: List kids with pagination, second page
        When I request the list of kids with page size 2 and page number 1
        Then I receive a page of 1 kids out of a total of 3

    Scenario: Get a kid
        When I request kid "Alicia" "Cristóbal"
        Then I receive the details of kid "Alicia" "Cristóbal"

    Scenario: Cannot get a kid that does not exist
        When I request a kid that does not exist
        Then I receive a not found error
