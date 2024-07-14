$(function () {

    const e = JSON.parse(document.getElementById("data").innerText);

    if(e.syncTemplate === 'on') {
        document.getElementById("syncTemplate").setAttribute('checked',"checked");
    }
    if(e.syncHtml === 'on'){
        document.getElementById("syncHtml").setAttribute('checked',"checked");
    }

    new Vue({
        el: '#vue-div',
        data: {
            config: e,
            version: e.version,
        },
        methods: {
            val: function (val) {
                return val;
            }
        }
    })

    $(".checkbox").map((e) => {
        $($(".checkbox")[e]).on('click', (event) => {
            if (event.target.checked) {
                document.getElementById(event.target.id+"Val").value = 'on';
            } else {
                document.getElementById(event.target.id+"Val").value = 'off';
            }
        });
    });

    $(".btn-primary").click(function () {
        var formId = "ajax" + $(this).attr("id");
        $.post('update', $("#" + formId).serialize().trim(), function (data) {
            if (data.success || data.status === 200) {
                $.gritter.add({
                    title: '  操作成功...',
                    class_name: 'gritter-success' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : ''),
                });
            } else {
                $.gritter.add({
                    title: '  发生了一些异常...',
                    class_name: 'gritter-error' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : ''),
                });
            }
        });
    });
});