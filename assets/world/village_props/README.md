# Existing artwork cutouts

These transparent sprites reuse unchanged pixels from assets/world/base_clean.png, village_clean.png and forest_clean.png. No replacement artwork was generated and no original PNG was edited.

Reproducible extraction: build/village-environment-polish/ExtractProps.java. It removes known terrain palette colors and retains the principal connected object. Cutouts use native resolution; the young-pine variant is cached with nearest-neighbor scaling by VillageEnvironment.

Village currently uses all cutouts except wooden_sign.png, which is available but not placed. These props are rendering-only, with no collision or gameplay data.
