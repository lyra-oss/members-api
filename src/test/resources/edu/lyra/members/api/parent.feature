Feature: Parents' onboarding

    In order to let parents use our service
    As Lyra
    I want to provide onboarding services

    Scenario: Create parent account
        Given my name is "Esteban"
        And my surname is "Cristóbal"
        And my e-mail address is "esteban.cristobal@example.com"
        When I click on "Create account"
        Then I receive a confirmation that my account has been successfully created

    # Tokens used in the examples below:
    # - missing: field will be omitted from the payload
    # - null: field will be explicitly set to JSON null
    # - empty: field will be an empty string ""
    # - spaces: field will be a string with three spaces
    # - a(101): expands to 101 repetitions of 'a' (same for b(101) and c(201))
    # - c(201)@example.com: expands to 201 'c' characters followed by @example.com
    Scenario Outline: Error creating parent account with invalid data
        Given my name is "<name>"
        And my surname is "<surname>"
        And my e-mail address is "<mail>"
        When I click on "Create account"
        Then I receive an error because my data is invalid

        Examples:
            | case                  | name    | surname   | mail                          |
            | Name too long         | a(101)  | Cristóbal | esteban.cristobal@example.com |
            | Surname too long      | Esteban | b(101)    | esteban.cristobal@example.com |
            | E-mail too long       | Esteban | Cristóbal | c(201)@example.com            |
            | Invalid e-mail format | Esteban | Cristóbal | not-an-email                  |
            | Missing name          | missing | Cristóbal | esteban.cristobal@example.com |
            | Null name             | null    | Cristóbal | esteban.cristobal@example.com |
            | Empty name            | empty   | Cristóbal | esteban.cristobal@example.com |
            | Whitespace name       | spaces  | Cristóbal | esteban.cristobal@example.com |
            | Missing surname       | Esteban | missing   | esteban.cristobal@example.com |
            | Null surname          | Esteban | null      | esteban.cristobal@example.com |
            | Empty surname         | Esteban | empty     | esteban.cristobal@example.com |
            | Whitespace surname    | Esteban | spaces    | esteban.cristobal@example.com |
            | Missing e-mail        | Esteban | Cristóbal | missing                       |
            | Null e-mail           | Esteban | Cristóbal | null                          |
            | Empty e-mail          | Esteban | Cristóbal | empty                         |
            | Whitespace e-mail     | Esteban | Cristóbal | spaces                        |

    Scenario: Create duplicated parent account
        Given my name is "José"
        And my surname is "García"
        And my e-mail address is "jose.garcia@example.com"
        And I already have an account
        When I click on "Create account"
        Then I receive an error because the account already exists
