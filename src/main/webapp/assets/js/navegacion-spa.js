
/**
 * Navegación entre módulos de VetCare.
 * Cada página se carga completa para actualizar correctamente
 * el encabezado, el menú activo y el contenido de cada módulo.
 */
(function () {
    "use strict";

    document.addEventListener("click", function (evento) {
        if (
            evento.defaultPrevented ||
            evento.button !== 0 ||
            evento.metaKey ||
            evento.ctrlKey ||
            evento.shiftKey ||
            evento.altKey
        ) {
            return;
        }

        var enlace = evento.target.closest("a");

        if (!enlace || !enlace.href) {
            return;
        }

        if (
            enlace.hasAttribute("data-bs-toggle") ||
            enlace.hasAttribute("data-no-ajax") ||
            enlace.target ||
            enlace.hasAttribute("download")
        ) {
            return;
        }

        var url;

        try {
            url = new URL(enlace.href, window.location.href);
        } catch (error) {
            return;
        }

        if (
            url.origin !== window.location.origin ||
            (url.protocol !== "http:" && url.protocol !== "https:")
        ) {
            return;
        }

        // Los enlaces internos con hash mantienen su comportamiento normal.
        if (
            url.pathname === window.location.pathname &&
            url.search === window.location.search &&
            url.hash
        ) {
            return;
        }

        // Deja que el navegador cargue el módulo completo.
        // Así se actualizan el encabezado, el menú y la vista.
    });
})();
