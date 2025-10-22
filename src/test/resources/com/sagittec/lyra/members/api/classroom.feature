Feature: Classrooms management

    In order to register kids under classrooms
    As Lyra
    I want to provide onboarding services for classrooms

    Scenario: Create classroom
        Given the classroom course is 3
        And the classroom group is "A"
        When I click on "Create classroom"
        Then I receive a confirmation that the classroom has been successfully created

    # Tokens used in the examples below:
    # - missing: field will be omitted from the payload
    # - null: field will be explicitly set to JSON null
    # - empty: field will be an empty string ""
    # - spaces: field will be a string with three spaces
    # - a(2): expands to 'aa' (used to exceed 1 character limit)
    Scenario Outline: Error creating classroom with invalid data
        Given the classroom course is <course>
        And the classroom group is "<group>"
        When I click on "Create classroom"
        Then I receive an error because the classroom data is invalid

        Examples:
            | case            | course | group |
            | Course too low  | 0      | A     |
            | Course too high | 7      | A     |
            | Course missing  | missing| A     |
            | Course null     | null   | A     |
            | Group empty     | 3      | empty |
            | Group spaces    | 3      | spaces|
            | Group lowercase | 3      | a     |
            | Group too long  | 3      | a(2)  |

    Scenario: Create duplicated classroom
        Given the classroom course is 4
        And the classroom group is "B"
        And this classroom already exists
        When I click on "Create classroom"
        Then I receive an error because the classroom already exists
