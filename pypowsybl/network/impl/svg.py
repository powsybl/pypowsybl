class Svg:
    """
    This class represents a single line diagram."""

    def __init__(self, content: str):
        self._content = content

    @property
    def svg(self) -> str:
        return self._content

    def __str__(self) -> str:
        return self._content

    def _repr_svg_(self) -> str:
        return self._content