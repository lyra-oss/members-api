Feature: Teachers directory

    In order to look up registered teachers
    As Lyra
    I want to list and retrieve teacher records

    Background:
        Given a school named "Gloria Fuertes" exists
        And a teacher named "Marta" "Ibáñez" exists at school "Gloria Fuertes" with e-mail "marta.ibanez@example.com"
        And a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And a teacher named "José" "García" exists at school "Gloria Fuertes" with e-mail "jose.garcia@example.com"
        And I am authenticated with "teachers.read" scope

    Scenario: List teachers
        When I request the list of teachers
        Then the list of teachers includes "Marta" "Ibáñez"

    Scenario: List teachers with pagination
        When I request the list of teachers with page size 2 and page number 0
        Then I receive a page of 2 teachers out of a total of 3

    Scenario: List teachers with pagination, second page
        When I request the list of teachers with page size 2 and page number 1
        Then I receive a page of 1 teachers out of a total of 3

    Scenario: Get a teacher
        When I request teacher "Marta" "Ibáñez"
        Then I receive the details of teacher "Marta" "Ibáñez"

    Scenario: Cannot get a teacher that does not exist
        When I request a teacher that does not exist
        Then I receive a not found error
