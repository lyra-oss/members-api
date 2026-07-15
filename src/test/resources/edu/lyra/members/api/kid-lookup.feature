Feature: Kids directory

    In order to look up registered kids
    As Lyra
    I want to list and retrieve kid records

    Background:
        Given the following kids exist:
            | name   | surname   | birthdate  |
            | Alicia | Cristóbal | 2019-12-12 |
            | Marta  | Ibáñez    | 2018-05-20 |
            | Pablo  | Ruiz      | 2020-03-08 |
            | Lucía  | Moreno    | 2017-09-15 |
            | Diego  | Torres    | 2021-01-30 |
        And I am authenticated with "kids.read" scope

    Scenario: List kids
        When I request the list of kids
        Then the list of kids includes "Alicia" "Cristóbal"

    Scenario: List kids with pagination
        When I request the list of kids with page size 2 and page number 0
        Then I receive a page of 2 kids out of a total of 5

    Scenario: List kids with pagination, second page
        When I request the list of kids with page size 2 and page number 1
        Then I receive a page of 2 kids out of a total of 5

    Scenario: Get a kid
        When I request kid "Alicia" "Cristóbal"
        Then I receive the details of kid "Alicia" "Cristóbal"

    Scenario: Cannot get a kid that does not exist
        When I request a kid that does not exist
        Then I receive a not found error
