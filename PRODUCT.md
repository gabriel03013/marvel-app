# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

The existing documents define a school-project audience exploring comic characters and building mission teams. The user confirmed those documents as the scope; no additional audience requirements were supplied.

## Product Purpose

S.H.I.E.L.D. Archives lets users search, discover and recruit using real Comic Vine data. Success is the complete navigation and persistence flow in SCREENS.md.

## Capabilities and Constraints

Kotlin with native Android XML layouts, explicitly requested by the user instead of Compose. Google and email/password authentication through Firebase, email signup and password recovery, Firestore profiles, favorites, teams and missions. English UI. Real API facts, graceful incomplete records, paginated requests, private local API configuration. Mission templates and clearly labeled app evaluations are permitted.

## Brand Commitments

Preserve the comic archive and recruitment idea, bundled fonts and English product. The user requested a full visual rebuild, intense collage on every screen and newly generated assets to replace the rejected PNGs. DESIGN.md records the resulting system. Do not use the official Marvel logo as created branding.

## Evidence on Hand

AGENTS.md, CONTEXT.md, DESIGN.md, SCREENS.md; new original RGBA collage assets, bundled licensed fonts, private Firebase/Comic Vine configuration, actual native capture matrix and separate emulator flow tests. Comic Vine key was absent at initial inspection and is now configured privately.

## Product Principles

- Deliver working discovery and recruitment before optional archive areas.
- Separate data, state and XML presentation.
- Preserve real facts and distinguish app evaluations.
- Make failures recoverable and navigation predictable.

## Accessibility & Inclusion

Readable text above texture, labeled imagery, 48dp actions, support for long names and missing data.
