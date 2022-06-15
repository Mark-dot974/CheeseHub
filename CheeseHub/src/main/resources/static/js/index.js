$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求（）POST
	$.post(
		CONTEXT_PATH + "/discuss/publish",
		{"title":title,"content":content},
		function (data){
			// 因为后端传递过来的是String类型，所以此处需要进行类型转换
			data = $.parseJSON(data);
			// 在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2s后自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
			}, 2000);
			// 刷新页面
			if(data.code == 0) {
				window.location.reload();
			}
		}
	)

	// 隐藏发布帖子的文本框
	$("#publishModal").modal("hide");
}