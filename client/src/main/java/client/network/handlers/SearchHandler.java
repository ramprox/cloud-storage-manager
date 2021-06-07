package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.responses.SearchResponse;
import io.netty.channel.*;

/**
 * Класс обработчика ответственный за обработку результата поиска файла на сервере
 */
public class SearchHandler extends SimpleChannelInboundHandler<SearchResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SearchResponse searchResponse) {
        Presentable presentable = Client.getPresentable();
        presentable.foundedFilesReceived(searchResponse.getFoundedFiles());
    }
}
