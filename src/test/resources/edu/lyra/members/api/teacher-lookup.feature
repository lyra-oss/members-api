Feature: Teachers directory

    In order to look up registered teachers
    As Lyra
    I want to list and retrieve teacher records

    Background:
        Given a school named "Gloria Fuertes" exists
        And the following teachers exist at school "Gloria Fuertes":
            | name  | surname | mail                     |
            | Marta | Ibáñez  | marta.ibanez@example.com |
            | Pablo | Ruiz    | pablo.ruiz@example.com   |
            | José  | García  | jose.garcia@example.com  |
            | Lucía | Moreno  | lucia.moreno@example.com |
            | Diego | Torres  | diego.torres@example.com |
        And I am authenticated with "teachers.read" scope

    Scenario: List teachers
        When I request the list of teachers
        Then the list of teachers includes "Marta" "Ibáñez"

    Scenario: List teachers with pagination
        When I request the list of teachers with page size 2 and page number 0
        Then I receive a page of 2 teachers out of a total of 5

    Scenario: List teachers with pagination, second page
        When I request the list of teachers with page size 2 and page number 1
        Then I receive a page of 2 teachers out of a total of 5

    Scenario: Get a teacher
        When I request teacher "Marta" "Ibáñez"
        Then I receive the details of teacher "Marta" "Ibáñez"

    Scenario: Cannot get a teacher that does not exist
        When I request a teacher that does not exist
        Then I receive a not found error
