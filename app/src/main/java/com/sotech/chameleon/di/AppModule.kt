package com.sotech.chameleon.di

import android.content.Context
import com.google.gson.Gson
import com.sotech.chameleon.data.ModelRepository
import com.sotech.chameleon.llm.LlmHelper
import com.sotech.chameleon.llm.GeminiHelper
import com.sotech.chameleon.execution.CodeExecutor
import com.sotech.chameleon.execution.CodeParser
import com.sotech.chameleon.execution.GraphGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideModelRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): ModelRepository = ModelRepository(context, gson)

    @Provides
    @Singleton
    fun provideLlmHelper(): LlmHelper = LlmHelper()

    @Provides
    @Singleton
    fun provideGeminiHelper(): GeminiHelper = GeminiHelper()

    @Provides
    @Singleton
    fun provideCodeExecutor(): CodeExecutor = CodeExecutor()

    @Provides
    @Singleton
    fun provideCodeParser(): CodeParser = CodeParser()

    @Provides
    @Singleton
    fun provideGraphGenerator(): GraphGenerator = GraphGenerator()
}