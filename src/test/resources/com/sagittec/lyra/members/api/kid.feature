Feature: Kids' onboarding

    In order to let kids be registered under their parents
    As Lyra
    I want to provide onboarding services for kids

    Scenario: Create kid account
        Given the kid name is "Alicia"
        And the kid surname is "Cristóbal"
        And the kid birthdate is "2019-12-12"
        When I click on "Create kid"
        Then I receive a confirmation that the kid has been successfully created

    # Tokens used in the examples below:
    # - missing: field will be omitted from the payload
    # - null: field will be explicitly set to JSON null
    # - empty: field will be an empty string ""
    # - spaces: field will be a string with three spaces
    # - a(101): expands to 101 repetitions of 'a' (same for b(101))
    # - future: expands to a date string in the future (tomorrow)
    Scenario Outline: Error creating kid with invalid data
        Given the kid name is "<name>"
        And the kid surname is "<surname>"
        And the kid birthdate is "<birthdate>"
        When I click on "Create kid"
        Then I receive an error because the kid data is invalid

        Examples:
            | case                 | name    | surname   | birthdate |
            | Name too long        | a(101)  | Cristóbal | valid     |
            | Surname too long     | Alicia  | b(101)    | valid     |
            | Missing name         | missing | Cristóbal | valid     |
            | Null name            | null    | Cristóbal | valid     |
            | Empty name           | empty   | Cristóbal | valid     |
            | Whitespace name      | spaces  | Cristóbal | valid     |
            | Missing surname      | Alicia  | missing   | valid     |
            | Null surname         | Alicia  | null      | valid     |
            | Empty surname        | Alicia  | empty     | valid     |
            | Whitespace surname   | Alicia  | spaces    | valid     |
            | Missing birthdate    | Alicia  | Cristóbal | missing   |
            | Null birthdate       | Alicia  | Cristóbal | null      |
            | Empty birthdate      | Alicia  | Cristóbal | empty     |
            | Whitespace birthdate | Alicia  | Cristóbal | spaces    |
            | Future birthdate     | Alicia  | Cristóbal | future    |
