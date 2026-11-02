/**
 * Wiring global de SweetAlert2 para toda la plantilla. No usa jQuery.
 * Se carga una sola vez (scripts.jspf) y aplica a cualquier página que traiga
 * el div #flash o formularios con data-confirmar / data-cargando / novalidate.
 */
(function () {
    "use strict";

    // Funciones que cualquier página puede usar desde su propio <script>
    window.Alerta = {
        exito: function (mensaje) {
            Swal.fire({
                toast: true,
                position: "top-end",
                timer: 2500,
                showConfirmButton: false,
                icon: "success",
                title: mensaje
            });
        },
        error: function (mensaje) {
            Swal.fire({ icon: "error", title: mensaje, confirmButtonColor: "#405189" });
        },
        // "peligro" (rojo #f06548): accion irreversible, ej. borrado definitivo.
        // "advertencia" (ambar #f7b84b, default): reversible, ej. desactivar.
        // El boton "Si" toma el mismo color que el boton que abrio el dialogo, para que la
        // confirmacion refuerce -no contradiga- la señal visual que ya vio el usuario.
        confirmar: function (mensaje, tipo) {
            var color = tipo === "peligro" ? "#f06548" : "#f7b84b";
            return Swal.fire({
                icon: "warning",
                title: mensaje,
                showCancelButton: true,
                confirmButtonText: "Sí",
                cancelButtonText: "Cancelar",
                confirmButtonColor: color
            }).then(function (resultado) {
                return resultado.isConfirmed;
            });
        }
    };

    // a) Mensaje flash guardado en sesión (viene de Flash.exito/error/advertencia).
    //    El éxito se muestra como toast (no interrumpe); error/advertencia como modal,
    //    para que una validación fallida no pase desapercibida.
    document.addEventListener("DOMContentLoaded", function () {
        var flash = document.getElementById("flash");
        if (!flash) {
            return;
        }
        var tipo = flash.dataset.tipo || "info";
        var mensaje = flash.dataset.msg;

        if (tipo === "success") {
            window.Alerta.exito(mensaje);
        } else {
            Swal.fire({ icon: tipo, title: mensaje, confirmButtonColor: "#405189" });
        }
    });

    // b) y d) Validación de formularios al enviarse
    document.addEventListener("submit", function (evento) {
        var form = evento.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }

        // d) Formularios marcados con novalidate: se apaga el globo nativo del navegador
        //    y el primer campo inválido se reporta con SweetAlert2 en su lugar.
        if (form.hasAttribute("novalidate") && !form.checkValidity()) {
            evento.preventDefault();
            var invalido = form.querySelector(":invalid");
            if (invalido) {
                window.Alerta.error(mensajeDeValidacion(invalido));
                invalido.focus();
            }
            return;
        }

        // b) Formularios que piden confirmación antes de enviarse (ej: botones Eliminar)
        if (form.dataset.confirmar && !form.dataset.confirmado) {
            evento.preventDefault();
            window.Alerta.confirmar(form.dataset.confirmar, form.dataset.confirmarTipo).then(function (confirmado) {
                if (confirmado) {
                    form.dataset.confirmado = "1";
                    form.requestSubmit();
                }
            });
            return;
        }

        // c) Formularios que muestran un loading mientras se procesa el POST
        if (form.dataset.cargando) {
            Swal.fire({
                title: "Procesando...",
                allowOutsideClick: false,
                didOpen: function () {
                    Swal.showLoading();
                }
            });
        }
    });

    // e) Botones que abren un modal de formulario (crear/editar), ej: data-bs-target="#modalUsuario".
    //    Copian sus atributos data-* a los campos del formulario con el mismo "name": así el botón
    //    "Editar" llena el modal sin ir al servidor. Si el botón no trae data-*, el formulario queda vacío.
    document.addEventListener("click", function (evento) {
        var boton = evento.target.closest('[data-bs-toggle="modal"]');
        if (!boton) {
            return;
        }
        var modal = document.querySelector(boton.getAttribute("data-bs-target"));
        var form = modal ? modal.querySelector("form") : null;
        if (!form) {
            return;
        }
        form.reset();
        Object.keys(boton.dataset).forEach(function (nombreCampo) {
            var campo = form.querySelector('[name="' + nombreCampo + '"]');
            if (campo) {
                campo.value = boton.dataset[nombreCampo];
            }
        });
    });

    // f) Si el servidor pide reabrir un modal (ej: falló una validación de servidor), se abre solo
    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-modal-autoabrir]").forEach(function (marcador) {
            var modal = document.getElementById(marcador.dataset.modalAutoabrir);
            if (modal) {
                new bootstrap.Modal(modal).show();
            }
        });
    });

    /** Arma un mensaje amigable en español a partir de la validación nativa del campo. */
    function mensajeDeValidacion(campo) {
        var etiqueta = obtenerEtiqueta(campo);
        var v = campo.validity;

        if (v.valueMissing) {
            return "El campo \"" + etiqueta + "\" es obligatorio.";
        }
        if (v.typeMismatch) {
            return "El campo \"" + etiqueta + "\" no tiene un formato válido.";
        }
        if (v.patternMismatch) {
            return "El campo \"" + etiqueta + "\" no tiene el formato esperado.";
        }
        if (v.tooShort) {
            return "El campo \"" + etiqueta + "\" debe tener al menos " + campo.minLength + " caracteres.";
        }
        return "Revisa el campo \"" + etiqueta + "\".";
    }

    /** Busca el texto del <label for="..."> asociado al campo, para nombrarlo en el mensaje. */
    function obtenerEtiqueta(campo) {
        if (campo.id) {
            var label = document.querySelector('label[for="' + campo.id + '"]');
            if (label) {
                return label.textContent.trim();
            }
        }
        return campo.name || "este campo";
    }
})();
