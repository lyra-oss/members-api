Feature: Schools' onboarding

    In order to register classrooms under a school
    As Lyra
    I want to provide onboarding services for schools

    Scenario: Create school
        Given the school name is "Colegio Público Cervantes"
        When I click on "Create school"
        Then I receive a confirmation that the school has been successfully created

    # Tokens used in the examples below:
    # - missing: field will be omitted from the payload
    # - null: field will be explicitly set to JSON null
    # - empty: field will be an empty string ""
    # - spaces: field will be a string with three spaces
    # - a(101): expands to 101 repetitions of 'a'
    Scenario Outline: Error creating school with invalid data
        Given the school name is "<name>"
        When I click on "Create school"
        Then I receive an error because the school data is invalid

        Examples:
            | case            | name   |
            | Name too long   | a(101) |
            | Missing name    | missing|
            | Null name       | null   |
            | Empty name      | empty  |
            | Whitespace name | spaces |
